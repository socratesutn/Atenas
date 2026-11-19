package ar.edu.utn.frc.atenas.catedra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades propias del cliente de Keycloak. La clave temporal se lee del
 * entorno y nunca se escribe en logs ni en el código.
 */
@Component
@ConfigurationProperties(prefix = "atenas.keycloak")
public class KeycloakProperties {

    private String claveTemporal;
    private Admin admin = new Admin();

    public String getClaveTemporal() {
        return claveTemporal;
    }

    public void setClaveTemporal(String claveTemporal) {
        this.claveTemporal = claveTemporal;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Admin {

        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
