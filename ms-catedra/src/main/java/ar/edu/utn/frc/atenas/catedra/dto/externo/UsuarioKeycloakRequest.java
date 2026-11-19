package ar.edu.utn.frc.atenas.catedra.dto.externo;

import java.util.List;

/**
 * Representación que espera la Admin API de Keycloak al crear un usuario.
 * Es un DTO de integración: no se usa en el dominio ni se comparte con otros
 * servicios.
 */
public record UsuarioKeycloakRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        List<CredencialKeycloak> credentials,
        List<String> requiredActions) {
}
