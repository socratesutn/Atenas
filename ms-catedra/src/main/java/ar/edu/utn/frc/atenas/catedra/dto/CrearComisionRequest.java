package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos necesarios para crear una comisión permanente.
 */
public record CrearComisionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre) {
}
