package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Datos públicos de un estudiante. No expone el sub de Keycloak.
 */
public record EstudianteResponse(
        Long id,
        String legajo,
        String nombre,
        String apellido,
        String mail,
        String usuarioGithub,
        boolean activo,
        Long comisionLectivaId) {
}
