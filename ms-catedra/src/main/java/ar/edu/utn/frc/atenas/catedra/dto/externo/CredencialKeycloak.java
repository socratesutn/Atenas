package ar.edu.utn.frc.atenas.catedra.dto.externo;

/**
 * Credencial inicial de Keycloak. {@code temporary=true} obliga al usuario a
 * cambiarla en el primer inicio.
 */
public record CredencialKeycloak(String type, String value, boolean temporary) {
}
