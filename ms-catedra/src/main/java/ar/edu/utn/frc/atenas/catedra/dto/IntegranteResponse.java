package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Integrante visible en las respuestas públicas de equipos.
 */
public record IntegranteResponse(
        Long estudianteId,
        String sub,
        String legajo,
        String usuarioGithub) {
}
