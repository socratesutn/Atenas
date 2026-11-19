package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.AsignacionDocenteResponse;
import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;

/**
 * Conversión manual de una asignación docente-comisión.
 */
public final class AsignacionDocenteMapper {

    private AsignacionDocenteMapper() {
    }

    public static AsignacionDocenteResponse aResponse(AsignacionDocente asignacion) {
        return new AsignacionDocenteResponse(
                asignacion.getId(),
                asignacion.getDocente().getId(),
                asignacion.getComisionLectiva().getId());
    }
}
