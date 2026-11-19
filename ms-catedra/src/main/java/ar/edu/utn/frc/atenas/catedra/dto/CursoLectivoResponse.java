package ar.edu.utn.frc.atenas.catedra.dto;

import java.time.LocalDate;

/**
 * Respuesta pública de un curso lectivo.
 */
public record CursoLectivoResponse(
        Long id,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}
