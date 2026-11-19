# Documentación de Atenas

Esta carpeta es la **fuente única de verdad**. Cada tema vive en un solo archivo; los demás lo referencian.

## Qué leer según lo que vas a hacer
| Objetivo | Ruta de lectura |
|---|---|
| Empezar a programar hoy | 11 → 07 → 08 → `AGENTS.md` del servicio → 06 → 03 |
| Entender el sistema | 01 → 04 → 09 → 05 |
| Preparar la defensa | 12 → 10 → adr → 13 |
| Saber qué cambió respecto del cuestionario | 14 |

## Índice
| Archivo | Contenido |
|---|---|
| `01-vision-y-alcance.md` | Problema, actores, alcance, proceso central, glosario |
| `02-requisitos.md` | Requisitos de la cátedra, funcionalidades F01–F16, requisitos no funcionales |
| `03-reglas-de-negocio.md` | Catálogo RN, máquinas de estados |
| `04-arquitectura.md` | C1/C2, responsabilidades, comunicación, seguridad, commons |
| `05-modelo-de-datos.md` | DER de los tres esquemas y decisiones de persistencia |
| `06-contratos-api.md` | Endpoints, matriz de permisos, errores, contratos internos |
| `07-estandar-microservicio.md` | Estructura, capas, convenciones, dependencias aprobadas |
| `08-testing.md` | Estrategia y criterios de tests |
| `09-flujos.md` | Diagramas de secuencia de los flujos principales |
| `10-patrones-de-diseno.md` | State y Adapter: dónde, por qué y por qué no otros |
| `11-guia-de-desarrollo.md` | Arranque, `.env`, puertos, Git, definición de terminado, plan y reparto |
| `12-guia-de-defensa.md` | Qué estudiar y preguntas probables |
| `13-trazabilidad-rubrica.md` | Criterio de la rúbrica → evidencia |
| `14-cambios-respecto-cuestionario.md` | Cambios, supuestos y pendientes de confirmar |
| `adr.md` | Decisiones arquitectónicas con alternativas |

## Reglas para mantener la documentación
- Un dato, un lugar. Si cambia una regla o un endpoint, se edita en su archivo y se anota en `adr.md` o en `14` si afecta lo comprometido con la cátedra.
- Los diagramas están en Mermaid (se ven en GitHub y en la mayoría de los editores).
- Los apuntes se citan como "Apunte N" del resumen de la cursada (el resumen 19-24 tiene dos "Apunte 21": se cita 21-Seguridad y 21-Docker).
