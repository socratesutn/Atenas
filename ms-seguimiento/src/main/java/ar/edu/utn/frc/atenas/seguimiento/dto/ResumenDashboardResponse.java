package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.util.List;

/**
 * Respuesta de S09: resumen de los equipos de una comisión o de un curso lectivo.
 *
 * <p>El campo {@code entregas} de la raíz informa si ms-documentacion estaba disponible al
 * armar el dashboard (RN33). Cada equipo repite el estado de entregas con solo sus filas.</p>
 */
public record ResumenDashboardResponse(
        Long comisionLectivaId,
        Long cursoLectivoId,
        EntregasDashboardResponse entregas,
        List<Equipo> equipos) {

    /** Datos resumidos de un equipo dentro del listado. */
    public record Equipo(
            Long equipoId,
            String nombre,
            String estado,
            AsistenciaResumenResponse asistencia,
            EntregasDashboardResponse entregas,
            ActividadDashboardResponse actividad) {
    }
}
