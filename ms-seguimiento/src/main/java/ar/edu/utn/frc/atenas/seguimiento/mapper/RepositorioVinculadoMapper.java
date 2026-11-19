package ar.edu.utn.frc.atenas.seguimiento.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.RepositorioVinculadoResponse;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;

/** Convierte la entidad {@link RepositorioVinculado} en su respuesta de la API. */
public final class RepositorioVinculadoMapper {

    private RepositorioVinculadoMapper() {
    }

    /** La fecha se guarda en UTC y se responde con el offset de la zona configurada. */
    public static RepositorioVinculadoResponse aResponse(RepositorioVinculado repositorio, ZonaHoraria zonaHoraria) {
        return new RepositorioVinculadoResponse(
                repositorio.getEquipoId(),
                repositorio.getUrl(),
                repositorio.getPropietario(),
                repositorio.getNombre(),
                repositorio.getRamaSeguimiento(),
                zonaHoraria.aOffsetDateTime(repositorio.getFechaValidacion()));
    }
}
