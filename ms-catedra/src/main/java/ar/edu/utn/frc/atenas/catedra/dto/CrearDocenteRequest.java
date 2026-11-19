package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos para dar de alta un docente. El mail será también el username y el
 * email de la cuenta de Keycloak.
 */
public record CrearDocenteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail no tiene un formato válido")
        String mail) {
}
