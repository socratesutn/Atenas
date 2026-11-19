package ar.edu.utn.frc.atenas.seguimiento.dto;

/**
 * Asistencia resumida que se muestra por equipo en S09.
 *
 * <p>{@code registros} cuenta las filas cargadas; {@code presentes}, las que marcan presencia.
 * Un evento sin fila todavía no tiene asistencia registrada.</p>
 */
public record AsistenciaResumenResponse(int eventos, int registros, int presentes) {
}
