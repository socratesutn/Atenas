package ar.edu.utn.frc.atenas.seguimiento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de la integración con GitHub (prefijo {@code atenas.github}).
 *
 * <p>El token llega por la variable de entorno {@code GITHUB_TOKEN}: nunca se
 * escribe en el código ni en los logs. Si está vacío, el cliente igual funciona
 * pero GitHub aplica la cuota de pedidos sin autenticar (mucho más baja).</p>
 */
@Component
@ConfigurationProperties(prefix = "atenas.github")
public class GitHubProperties {

    private String urlBase = "https://api.github.com";
    private String versionApi = "2022-11-28";
    private String token = "";

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public String getVersionApi() {
        return versionApi;
    }

    public void setVersionApi(String versionApi) {
        this.versionApi = versionApi;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
