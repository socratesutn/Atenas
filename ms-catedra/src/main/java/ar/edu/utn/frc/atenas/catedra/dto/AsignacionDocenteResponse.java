package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Respuesta de una asignación docente-comisión.
 */
public record AsignacionDocenteResponse(
        Long id,
        Long docenteId,
        Long comisionLectivaId) {
}
