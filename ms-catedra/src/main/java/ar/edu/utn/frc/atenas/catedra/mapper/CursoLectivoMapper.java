package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoResponse;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;

/**
 * Conversión manual entre {@link CursoLectivo} y sus DTO. Se mantiene separada
 * de la entidad para que JPA no dependa del contrato HTTP.
 */
public final class CursoLectivoMapper {

    private CursoLectivoMapper() {
    }

    public static CursoLectivoResponse aResponse(CursoLectivo curso) {
        return new CursoLectivoResponse(
                curso.getId(),
                curso.getAnio(),
                curso.getFechaInicio(),
                curso.getFechaFin());
    }

    public static CursoLectivoInternoResponse aInternoResponse(CursoLectivo curso) {
        return new CursoLectivoInternoResponse(
                curso.getId(),
                curso.getAnio(),
                curso.getFechaInicio(),
                curso.getFechaFin());
    }
}
