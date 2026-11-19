package ar.edu.utn.frc.atenas.catedra.config;

import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient propio para la Admin API de Keycloak. No reutiliza el builder de
 * atenas-commons porque ese interceptor propaga el JWT del usuario y pisaría
 * el token de servicio que necesita la Admin API.
 */
@Configuration
public class KeycloakHttpConfig {

    @Bean
    @Qualifier("keycloakRestClientBuilder")
    public RestClient.Builder keycloakRestClientBuilder(AtenasHttpProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Math.toIntExact(propiedades.getTimeoutConexion().toMillis()));
        fabrica.setReadTimeout(Math.toIntExact(propiedades.getTimeoutLectura().toMillis()));

        return RestClient.builder().requestFactory(fabrica);
    }
}
