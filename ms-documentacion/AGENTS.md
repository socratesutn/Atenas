# AGENTS.md — ms-documentacion

Leé primero el `AGENTS.md` de la raíz y `docs/07-estandar-microservicio.md`.

## Responsabilidad
Hitos del curso (los crea el **jefe**), entregas de los equipos y su estado, verificación del archivo en Google Drive, corrección y nota.

## Datos
Puerto dev **8083** · esquema **`documentacion`** (usuario `documentacion_user`) · tablas `hito`, `entrega`, `archivo_verificado` (`docs/05`).

## Qué implementa
- Endpoints públicos **D01–D07** y contratos internos **I06–I07** (`docs/06-contratos-api.md`).
- Reglas: RN17–RN24, RN29, RN30 (`docs/03`).
- Funcionalidades: F06, F07, F08, F09, F10.

## Depende de
- `ms-catedra` (por `CatedraClient`; rutas I01, I03, I04, I05) para validar curso y fechas del hito, equipo, representante y docente. Si falla → **503**.
- Google Drive API v3 (por `DriveClient`, con `DRIVE_API_KEY`). Al entregar, si falla → 503 y no registra (RN22); al abrir una entrega, si falla → 200 con `NO_VERIFICADO` (RN30).

## Particularidades
- `PENDIENTE` **no se persiste**: se calcula combinando los hitos del curso con las entregas de los equipos (`docs/adr.md`, ADR-010). I07 devuelve también las pendientes.
- Crear hito (RN17): solo el jefe; consultar el curso a `ms-catedra` (I05) para validar que existe (404) y que las fechas caen dentro de su período (400).
- Registrar entrega (RN18): el hito debe ser del curso del equipo (curso que informa I01). Consultar `ms-catedra` y Drive **antes** de la transacción; luego guardar `entrega` + `archivo_verificado` juntas (`TransactionTemplate`). `UNIQUE(hito_id, equipo_id)` es la garantía final contra duplicados (409, RN29).
- Comparación de cambios (RN30): `modified_time_entrega` no se sobrescribe; se calcula al abrir y se **congela** en `modificado_post_entrega` al corregir (queda `NULL` si Drive falla en ese momento). Es informativa y nunca bloquea.
- `DriveClient`: extrae el `fileId` del enlace (400 si no se reconoce), valida existencia, acceso público y mime type (PDF o Google Doc → 422 si no), y devuelve `modifiedTime`. El permiso "comentar" **no se verifica** (instrucción operativa a los alumnos).
- Nota entera de 1 a 10 (422 si no). Corregir solo sobre `ENTREGADA` (409 si ya corregida, 404 si pendiente).

## Tests propios
Unitarios de `service` por RN (RN17–RN24, RN29, RN30), `DriveClient` con `MockRestServiceServer` (200, 404, 403, 5xx, timeout, respuesta inesperada), `@WebMvcTest` por controlador.

## Este servicio NO
Almacena el contenido de los archivos, conoce la pertenencia a equipos (la pregunta), ni expone `/internal` al Gateway.
