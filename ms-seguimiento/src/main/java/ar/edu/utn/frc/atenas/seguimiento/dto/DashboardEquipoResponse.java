package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.util.List;

/**
 * Respuesta de S10: las tres secciones del dashboard de un equipo.
 *
 * <p>Asistencia y actividad salen de las tablas propias de ms-seguimiento; entregas se compone
 * con I07 y puede venir no disponible sin que eso cambie el código HTTP (RN33).</p>
 */
public record DashboardEquipoResponse(
        Long equipoId,
        String nombre,
        Long comisionLectivaId,
        Long cursoLectivoId,
        AsistenciaDashboardResponse asistencia,
        EntregasDashboardResponse entregas,
        ActividadDashboardResponse actividad) {
}
