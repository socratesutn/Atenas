package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear un espacio de equipo. El equipo nace ABIERTO.
 */
public record CrearEquipoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El cupo mínimo es obligatorio")
        @Min(value = 1, message = "El cupo mínimo debe ser al menos 1")
        Integer cupoMinimo,

        @NotNull(message = "El cupo máximo es obligatorio")
        @Min(value = 1, message = "El cupo máximo debe ser al menos 1")
        Integer cupoMaximo) {
}
