package ar.edu.utn.frc.atenas.catedra.dto;

import java.time.LocalDate;

/**
 * Contrato interno I05: datos mínimos del curso lectivo que necesita
 * ms-documentacion para validar las fechas de un hito.
 */
public record CursoLectivoInternoResponse(
        Long id,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}
