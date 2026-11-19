package ar.edu.utn.frc.atenas.documentacion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiDocumentacion() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de ms-documentacion")
                        .version("1.0.0")
                        .description("Hitos, entregas, verificación en Drive y corrección"));
    }
}
