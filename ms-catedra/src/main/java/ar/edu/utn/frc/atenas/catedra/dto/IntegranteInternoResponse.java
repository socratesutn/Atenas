package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Integrante con la forma exacta que consume I01/I02.
 */
public record IntegranteInternoResponse(
        Long estudianteId,
        String sub,
        String legajo,
        String usuarioGithub) {
}
