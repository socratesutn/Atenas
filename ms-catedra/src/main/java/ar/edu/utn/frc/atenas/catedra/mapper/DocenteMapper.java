package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.DocenteResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;

/**
 * Conversión manual entre {@link Docente} y su DTO público.
 */
public final class DocenteMapper {

    private DocenteMapper() {
    }

    public static DocenteResponse aResponse(Docente docente) {
        return new DocenteResponse(
                docente.getId(),
                docente.getNombre(),
                docente.getApellido(),
                docente.getMail(),
                docente.isActivo());
    }
}
