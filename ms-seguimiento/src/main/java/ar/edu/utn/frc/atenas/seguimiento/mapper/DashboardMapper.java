package ar.edu.utn.frc.atenas.seguimiento.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.ActividadDashboardResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.AsistenciaDashboardResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.AsistenciaResumenResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.DashboardEquipoResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.EntregasDashboardResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.ResumenDashboardResponse;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard;

import java.util.List;

/** Convierte los resultados internos del dashboard al contrato JSON de S09 y S10. */
public final class DashboardMapper {

    private DashboardMapper() {
    }

    public static ResumenDashboardResponse aResumen(ResultadosDashboard.ResumenEquipos resultado,
                                                    ZonaHoraria zonaHoraria) {
        List<ResumenDashboardResponse.Equipo> equipos = resultado.equipos().stream()
                .map(equipo -> new ResumenDashboardResponse.Equipo(
                        equipo.datos().id(),
                        equipo.datos().nombre(),
                        equipo.datos().estado(),
                        aAsistenciaResumen(equipo.asistencia()),
                        aEntregas(equipo.entregas()),
                        aActividad(equipo.actividad(), zonaHoraria)))
                .toList();
        return new ResumenDashboardResponse(
                resultado.comisionLectivaId(), resultado.cursoLectivoId(),
                aEntregas(resultado.entregas()), equipos);
    }

    public static DashboardEquipoResponse aDetalle(ResultadosDashboard.DetalleEquipo resultado,
                                                   ZonaHoraria zonaHoraria) {
        return new DashboardEquipoResponse(
                resultado.datos().id(),
                resultado.datos().nombre(),
                resultado.datos().comisionLectivaId(),
                resultado.datos().cursoLectivoId(),
                aAsistencia(resultado.asistencia(), zonaHoraria),
                aEntregas(resultado.entregas()),
                aActividad(resultado.actividad(), zonaHoraria));
    }

    private static AsistenciaResumenResponse aAsistenciaResumen(ResultadosDashboard.AsistenciaEquipo asistencia) {
        return new AsistenciaResumenResponse(asistencia.eventos(), asistencia.registros(), asistencia.presentes());
    }

    private static AsistenciaDashboardResponse aAsistencia(ResultadosDashboard.AsistenciaEquipo asistencia,
                                                           ZonaHoraria zonaHoraria) {
        List<AsistenciaDashboardResponse.Detalle> detalle = asistencia.detalle().stream()
                .map(fila -> new AsistenciaDashboardResponse.Detalle(
                        fila.getEvento().getId(),
                        zonaHoraria.aOffsetDateTime(fila.getEvento().getFecha()),
                        fila.getEvento().getTipo().name(),
                        fila.getEstudianteSub(),
                        fila.isPresente()))
                .toList();
        return new AsistenciaDashboardResponse(
                asistencia.eventos(), asistencia.registros(), asistencia.presentes(), detalle);
    }

    private static EntregasDashboardResponse aEntregas(ResultadosDashboard.EntregasEquipo entregas) {
        List<EntregasDashboardResponse.Entrega> hitos = entregas.entregas().stream()
                .map(entrega -> new EntregasDashboardResponse.Entrega(
                        entrega.hitoId(), entrega.estado(), entrega.tardia(), entrega.nota()))
                .toList();
        return new EntregasDashboardResponse(entregas.disponible(), hitos);
    }

    private static ActividadDashboardResponse aActividad(ResultadosDashboard.ActividadEquipo actividad,
                                                         ZonaHoraria zonaHoraria) {
        List<ActividadDashboardResponse.Snapshot> snapshots = actividad.snapshots().stream()
                .map(snapshot -> new ActividadDashboardResponse.Snapshot(
                        snapshot.hitoId(),
                        snapshot.estado(),
                        zonaHoraria.aOffsetDateTime(snapshot.fechaConsulta()),
                        snapshot.commitsTotales(),
                        snapshot.commitsSinAtribuir(),
                        snapshot.sinActividad(),
                        snapshot.integrantes().stream()
                                .map(integrante -> new ActividadDashboardResponse.Integrante(
                                        integrante.estudianteSub(), integrante.usuarioGithub(),
                                        integrante.commits(), integrante.bajaContribucion()))
                                .toList()))
                .toList();
        return new ActividadDashboardResponse(actividad.disponible(), snapshots);
    }
}
