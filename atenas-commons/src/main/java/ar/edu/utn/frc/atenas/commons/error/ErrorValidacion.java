package ar.edu.utn.frc.atenas.commons.error;

/**
 * Detalle de un error de validación de Bean Validation.
 */
public record ErrorValidacion(String campo, String mensaje) {
}
