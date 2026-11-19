package ar.edu.utn.frc.atenas.catedra.dto.externo;

/**
 * Rol de realm devuelto por Keycloak. El id es necesario para asignarlo a un
 * usuario mediante la Admin API.
 */
public record RolKeycloak(String id, String name) {
}
