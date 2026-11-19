package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Usuario de GitHub que carga el propio alumno. Se aceptan mayúsculas, pero el
 * service normaliza a minúsculas antes de guardar.
 */
public record ActualizarUsuarioGithubRequest(
        @NotBlank(message = "El usuario de GitHub es obligatorio")
        @Pattern(
                regexp = "^[A-Za-z0-9-]{1,39}$",
                message = "El usuario de GitHub solo admite letras, dígitos y guiones, hasta 39 caracteres")
        String usuarioGithub) {
}
