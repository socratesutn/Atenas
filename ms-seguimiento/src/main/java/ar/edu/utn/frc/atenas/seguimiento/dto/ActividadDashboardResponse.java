package ar.edu.utn.frc.atenas.seguimiento.dto;

import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Sección de actividad del dashboard: el último snapshot de cada sprint conocido.
 *
 * <p>Las marcas no se guardan: {@code sinActividad} y {@code bajaContribucion} se recalculan
 * cada vez que se consulta, a partir de los contadores del snapshot (RN27).</p>
 */
public record ActividadDashboardResponse(boolean disponible, List<Snapshot> snapshots) {

    /** Último snapshot de un sprint, con el estado de RN28 y las marcas informativas. */
    public record Snapshot(
            Long hitoId,
            EstadoSnapshot estado,
            OffsetDateTime fechaConsulta,
            Integer commitsTotales,
            Integer commitsSinAtribuir,
            boolean sinActividad,
            List<Integrante> integrantes) {
    }

    /** Contribución de un integrante dentro del snapshot. */
    public record Integrante(
            String estudianteSub,
            String usuarioGithub,
            int commits,
            boolean bajaContribucion) {
    }
}
