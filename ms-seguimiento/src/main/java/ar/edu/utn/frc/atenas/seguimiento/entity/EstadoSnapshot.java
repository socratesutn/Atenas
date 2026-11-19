package ar.edu.utn.frc.atenas.seguimiento.entity;

/**
 * Estado de un snapshot de actividad (docs/03, "Estados del snapshot").
 *
 * <ul>
 *   <li>{@link #ACTUALIZADO}: la consulta a GitHub salió bien.</li>
 *   <li>{@link #DESACTUALIZADO}: GitHub o un servicio interno no respondió; se conservan los
 *       datos de la última consulta buena.</li>
 *   <li>{@link #NO_DISPONIBLE}: no hay datos para mostrar (nunca hubo, o GitHub respondió
 *       algo inesperado); los contadores quedan en {@code null}.</li>
 * </ul>
 */
public enum EstadoSnapshot {
    ACTUALIZADO,
    DESACTUALIZADO,
    NO_DISPONIBLE
}
