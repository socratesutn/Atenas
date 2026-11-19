# 10 — Patrones de diseño aplicados

> Para estudiar: repositorio `BE-EXAMPLE-design-patterns-main`, carpetas `state/` y `adapter/` (cada una con su README y un ejemplo ejecutable). Los resúmenes de la cursada no cubren patrones.
> Criterio del equipo: un patrón solo entra si resuelve un problema real. Se aplican **dos**: State y Adapter.

## 1. State — estados del `Equipo`
**Problema.** El equipo está `ABIERTO` o `ASEGURADO` y la validez de varias operaciones depende de ese estado: ingresar (RN08), salir o rechazar (RN09), baja de un integrante (RN05), asegurar (RN10), reabrir (RN11), registrar entrega (RN18). Con `if` sobre el estado, esa lógica se repetiría en varios métodos del `service`.

**Diseño.**
```mermaid
classDiagram
    class Equipo {
        -EstadoEquipo estado
        +ingresar(estudiante)
        +salir(estudiante)
        +asegurar()
        +reabrir()
        +permiteEntregas() boolean
    }
    class EstadoEquipo {
        <<enum persistido>>
        ABIERTO
        ASEGURADO
        +comportamiento() ComportamientoEquipo
    }
    class ComportamientoEquipo {
        <<interface>>
        +ingresar(equipo, estudiante)
        +salir(equipo, estudiante)
        +asegurar(equipo)
        +reabrir(equipo)
        +permiteEntregas() boolean
    }
    Equipo --> EstadoEquipo
    EstadoEquipo ..> ComportamientoEquipo
    ComportamientoEquipo <|.. Abierto
    ComportamientoEquipo <|.. Asegurado
```
- Se **persiste solo el enum** (`equipo.estado`). Cada valor entrega su objeto de comportamiento (sin estado propio).
- `Abierto` permite ingresar, salir y asegurar; rechaza reabrir y las entregas (409). `Asegurado` permite reabrir y las entregas; rechaza ingresar, salir y asegurar (409).
- La regla de *quién* puede (docente o representante, RN10) y las condiciones de cupo viven en el `service`; el estado solo decide **si la operación es válida**.
- Se prueba con unitarios puros (08).

**Honestidad para la defensa.** Con dos estados la ganancia del patrón es modesta: un enum con guardas también funcionaría. Se mantiene porque concentra en un lugar reglas que hoy están repartidas (RN05, RN08–RN11, RN18), evita `if` duplicados y permite agregar un estado sin tocar los `service`. Se acota a una interfaz y dos clases para no caer en sobreingeniería.

**Por qué no en `Entrega` ni `Snapshot`.** `Entrega` tiene dos estados persistidos y una sola restricción (corregir solo si `ENTREGADA`): una guarda alcanza. En `Snapshot` los estados son datos del resultado, no cambian el comportamiento.

## 2. Adapter — acceso a otros servicios y sistemas externos
**Problema.** Los servicios hablan con sistemas que no controlamos (Drive, GitHub, Keycloak) y con otros servicios nuestros, cada uno con su formato, autenticación, paginación y errores. Si el `service` los usara directo, las reglas de negocio quedarían atadas a esos detalles y no se podrían testear sin red.

**Diseño.** El `service` depende de una **interfaz propia** en `client/`; una implementación con `RestClient` adapta el contrato externo al modelo del servicio.

| Interfaz | Servicio | Adapta | Esconde |
|---|---|---|---|
| `CatedraClient` | `ms-documentacion`, `ms-seguimiento` | API interna de `ms-catedra` | URLs, JWT propagado, correlation-id |
| `DocumentacionClient` | `ms-seguimiento` | API interna de `ms-documentacion` | ídem |
| `DriveClient` | `ms-documentacion` | Google Drive API v3 | API key, parseo del enlace, `modifiedTime`, mime types |
| `GitHubClient` | `ms-seguimiento` | GitHub REST | Token, **paginación** de commits, detección de merges, usuario del autor |
| `KeycloakAdminClient` | `ms-catedra` | Keycloak Admin API | Token de servicio, crear/deshabilitar usuario, asignar rol |

Beneficios concretos: (1) los DTO externos (`dto/externo/`) no entran al dominio; (2) la traducción de errores a `NoEncontradoException` / `ServicioNoDisponibleException` está en un único lugar; (3) los `service` se testean con Mockito sin red; (4) si cambia la API externa, se toca una clase.

## Patrones considerados y descartados
| Patrón | Por qué no |
|---|---|
| Iterator | La paginación de GitHub se resuelve con un bucle dentro de `GitHubClient`; un `Iterator` propio no simplifica nada |
| Observer | La comunicación es síncrona y no hay notificaciones (fuera de alcance) |
| Strategy | No hay familias de algoritmos intercambiables en tiempo de ejecución |
| Singleton | Ya lo provee el contenedor de Spring (scope `singleton`, Apunte 15); implementarlo a mano sería ruido |
| Builder | Lo da Lombok (`@Builder`) en entidades; no se diseña uno propio |

## Principios que sostienen el diseño
Separación de responsabilidades (capas), inversión de dependencias (el `service` depende de interfaces `client`), abierto/cerrado (nuevo estado = nueva clase), un solo motivo de cambio por clase.
