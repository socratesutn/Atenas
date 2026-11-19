package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;

/**
 * Conversión manual entre {@link Comision} y su DTO público.
 */
public final class ComisionMapper {

    private ComisionMapper() {
    }

    public static ComisionResponse aResponse(Comision comision) {
        return new ComisionResponse(comision.getId(), comision.getNombre());
    }
}
