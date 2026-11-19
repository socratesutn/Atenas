package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para dar de alta un estudiante. El mail no se recibe: se deriva del
 * legajo con el dominio institucional.
 */
public record CrearEstudianteRequest(
        @NotBlank(message = "El legajo es obligatorio")
        @Pattern(regexp = "\\d+", message = "El legajo debe contener solo dígitos")
        String legajo,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotNull(message = "La comisión lectiva es obligatoria")
        Long comisionLectivaId) {
}
