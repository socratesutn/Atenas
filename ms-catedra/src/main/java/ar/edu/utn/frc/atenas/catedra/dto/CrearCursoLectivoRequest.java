package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Datos necesarios para crear un curso lectivo. Las fechas se reciben como
 * fecha calendario porque el modelo las guarda como {@code date}.
 */
public record CrearCursoLectivoRequest(
        @NotNull(message = "El año es obligatorio")
        Integer anio,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin) {
}
