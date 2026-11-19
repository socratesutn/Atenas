package ar.edu.utn.frc.atenas.documentacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del cliente de Google Drive.
 *
 * La API key no se loguea ni se expone en la respuesta de la API.
 */
@Component
@ConfigurationProperties(prefix = "atenas.drive")
public class DriveProperties {

    private String urlBase = "https://www.googleapis.com/drive/v3";
    private String apiKey = "";

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
