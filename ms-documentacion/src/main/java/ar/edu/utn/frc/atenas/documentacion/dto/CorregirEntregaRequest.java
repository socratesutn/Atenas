package ar.edu.utn.frc.atenas.documentacion.dto;

/**
 * Body de D07.
 *
 * El rango se valida en el service para devolver 422 y no 400, según RN24.
 */
public record CorregirEntregaRequest(Integer nota) {
}
