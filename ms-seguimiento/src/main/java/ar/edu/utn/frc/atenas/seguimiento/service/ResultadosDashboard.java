package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;

import java.time.Instant;
import java.util.List;

/**
 * Resultados internos del dashboard, antes de convertirlos al contrato JSON.
 *
 * <p>El servicio compone acá entidades propias y DTO de integración; el mapper es el único
 * que conoce la forma final de la respuesta y el offset de Argentina.</p>
 */
public final class ResultadosDashboard {

    private ResultadosDashboard() {
    }

    public record AsistenciaEquipo(int eventos, int registros, int presentes, List<Asistencia> detalle) {
    }

    public record EntregasEquipo(boolean disponible, List<EntregaEquipoDto> entregas) {
    }

    public record ContribucionEquipo(String estudianteSub, String usuarioGithub, int commits,
                                     boolean bajaContribucion) {
    }

    public record SnapshotEquipo(Long hitoId, EstadoSnapshot estado, Instant fechaConsulta,
                                 Integer commitsTotales, Integer commitsSinAtribuir,
                                 boolean sinActividad, List<ContribucionEquipo> integrantes) {
    }

    public record ActividadEquipo(boolean disponible, List<SnapshotEquipo> snapshots) {
    }

    public record DetalleEquipo(EquipoDto datos, AsistenciaEquipo asistencia,
                                EntregasEquipo entregas, ActividadEquipo actividad) {
    }

    public record ResumenEquipos(Long comisionLectivaId, Long cursoLectivoId,
                                 EntregasEquipo entregas, List<DetalleEquipo> equipos) {
    }
}
