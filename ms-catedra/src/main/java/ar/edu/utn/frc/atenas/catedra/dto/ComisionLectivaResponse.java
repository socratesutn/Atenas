package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Respuesta de una comisión dictada en un curso lectivo. Incluye el nombre de
 * la comisión para que el cliente no tenga que hacer otra consulta.
 */
public record ComisionLectivaResponse(
        Long id,
        Long comisionId,
        String comisionNombre,
        Long cursoLectivoId) {
}
