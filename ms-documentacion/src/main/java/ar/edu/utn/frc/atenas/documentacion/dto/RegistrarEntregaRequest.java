package ar.edu.utn.frc.atenas.documentacion.dto;

import jakarta.validation.constraints.NotBlank;

/** Body de D03. */
public record RegistrarEntregaRequest(@NotBlank String enlace) {
}
