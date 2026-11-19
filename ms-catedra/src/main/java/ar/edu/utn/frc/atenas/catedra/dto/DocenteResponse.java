package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Datos públicos de un docente. El {@code sub} de Keycloak no se expone en
 * esta API; se usa en los contratos internos.
 */
public record DocenteResponse(
        Long id,
        String nombre,
        String apellido,
        String mail,
        boolean activo) {
}
