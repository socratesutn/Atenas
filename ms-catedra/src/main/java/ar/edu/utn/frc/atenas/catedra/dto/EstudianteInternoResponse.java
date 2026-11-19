package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Contrato interno I03: pertenencia vigente del alumno. {@code equipoId} será
 * null hasta que existan los equipos.
 */
public record EstudianteInternoResponse(
        Long estudianteId,
        String sub,
        boolean activo,
        Long comisionLectivaId,
        Long cursoLectivoId,
        Long equipoId) {
}
