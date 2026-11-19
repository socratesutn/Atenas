package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Perfil propio del alumno. El equipo queda en {@code null} hasta que se
 * implemente la etapa de equipos (A-E4).
 */
public record MiPerfilResponse(
        Long estudianteId,
        String legajo,
        String nombre,
        String apellido,
        String mail,
        String usuarioGithub,
        boolean activo,
        Long comisionLectivaId,
        String comisionNombre,
        Long cursoLectivoId,
        Integer cursoAnio,
        Long equipoId) {
}
