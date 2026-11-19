package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionLectivaResponse;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;

/**
 * Conversión manual de una comisión lectiva. Se accede a comisión y curso
 * dentro de la transacción del service, donde las relaciones LAZY están
 * inicializadas por el {@code EntityGraph} del repositorio.
 */
public final class ComisionLectivaMapper {

    private ComisionLectivaMapper() {
    }

    public static ComisionLectivaResponse aResponse(ComisionLectiva comisionLectiva) {
        return new ComisionLectivaResponse(
                comisionLectiva.getId(),
                comisionLectiva.getComision().getId(),
                comisionLectiva.getComision().getNombre(),
                comisionLectiva.getCursoLectivo().getId());
    }
}
