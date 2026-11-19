package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para asignar un docente existente a una comisión lectiva. La comisión
 * se recibe por la ruta.
 */
public record AsignarDocenteRequest(
        @NotNull(message = "El docente es obligatorio")
        Long docenteId) {
}
