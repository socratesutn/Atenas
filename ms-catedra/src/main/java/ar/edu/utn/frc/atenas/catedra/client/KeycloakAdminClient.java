package ar.edu.utn.frc.atenas.catedra.client;

/**
 * Adaptador hacia Keycloak Admin API. El service depende de esta interfaz para
 * poder probar las reglas de alta y baja sin levantar Keycloak.
 */
public interface KeycloakAdminClient {

    String crearUsuario(String mail, String nombre, String apellido, String claveTemporal);

    void asignarRolRealm(String sub, String rol);

    void deshabilitarUsuario(String sub);
}
