package ar.edu.utn.frc.atenas.commons.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuración base de {@link RestClient} para las llamadas entre servicios:
 * timeouts de conexión y lectura, propagación de JWT y correlation-id, y
 * traducción centralizada de respuestas de error.
 */
@Configuration
@EnableConfigurationProperties(AtenasHttpProperties.class)
public class ConfiguracionHttp {

    @Bean
    public RestClient.Builder restClientBuilder(AtenasHttpProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Math.toIntExact(propiedades.getTimeoutConexion().toMillis()));
        fabrica.setReadTimeout(Math.toIntExact(propiedades.getTimeoutLectura().toMillis()));

        return RestClient.builder()
                .requestFactory(fabrica)
                .requestInterceptor(new InterceptorHttpAtenas())
                .defaultStatusHandler(HttpStatusCode::isError,
                        (solicitud, respuesta) -> TraductorErroresHttp.evaluar(respuesta));
    }
}
