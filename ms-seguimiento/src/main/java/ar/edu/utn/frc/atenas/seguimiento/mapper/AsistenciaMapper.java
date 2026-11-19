package ar.edu.utn.frc.atenas.seguimiento.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.AsistenciaResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.MiAsistenciaResponse;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;

import java.util.List;

/** Convierte {@link Asistencia} en sus respuestas de la API. */
public final class AsistenciaMapper {

    private AsistenciaMapper() {
    }

    public static AsistenciaResponse aResponse(Asistencia asistencia, ZonaHoraria zonaHoraria) {
        return new AsistenciaResponse(
                // getId() de un proxy perezoso no dispara una consulta.
                asistencia.getEvento().getId(),
                asistencia.getEstudianteSub(),
                asistencia.getEquipoId(),
                asistencia.isPresente(),
                zonaHoraria.aOffsetDateTime(asistencia.getFechaRegistro()));
    }

    public static List<AsistenciaResponse> aResponses(List<Asistencia> asistencias, ZonaHoraria zonaHoraria) {
        return asistencias.stream().map(asistencia -> aResponse(asistencia, zonaHoraria)).toList();
    }

    /** Para S06 se necesitan los datos del evento, que el repositorio trae con {@code join fetch}. */
    public static MiAsistenciaResponse aMiAsistencia(Asistencia asistencia, ZonaHoraria zonaHoraria) {
        return new MiAsistenciaResponse(
                asistencia.getEvento().getId(),
                asistencia.getEvento().getComisionLectivaId(),
                asistencia.getEvento().getTipo().name(),
                zonaHoraria.aOffsetDateTime(asistencia.getEvento().getFecha()),
                asistencia.getEvento().getDescripcion(),
                asistencia.isPresente());
    }

    public static List<MiAsistenciaResponse> aMisAsistencias(List<Asistencia> asistencias, ZonaHoraria zonaHoraria) {
        return asistencias.stream().map(asistencia -> aMiAsistencia(asistencia, zonaHoraria)).toList();
    }
}
