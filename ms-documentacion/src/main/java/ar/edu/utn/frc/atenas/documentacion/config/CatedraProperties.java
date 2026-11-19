package ar.edu.utn.frc.atenas.documentacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URL base de ms-catedra para las llamadas internas.
 *
 * El valor por defecto permite ejecutar desde el IDE con el perfil dev. El
 * perfil docker lo reemplaza por el nombre del servicio dentro de Compose.
 */
@Component
@ConfigurationProperties(prefix = "atenas.clientes.catedra")
public class CatedraProperties {

    private String url = "http://localhost:8082";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
