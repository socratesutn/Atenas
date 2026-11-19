package ar.edu.utn.frc.atenas.catedra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Configura el contrato OpenAPI que expone Swagger UI.
     *
     * El servidor se declara como relativo ("/") para que Swagger use el mismo
     * origen desde donde se abrió la documentación. Si se dejara el valor
     * automático, el servicio vería el host interno de Docker (ms-catedra:8080)
     * y el navegador no podría llamar a los endpoints al usar "Try it out".
     *
     * También se declara el esquema Bearer JWT para que Swagger muestre el botón
     * "Authorize" y permita probar endpoints protegidos pegando un access token.
     */
    @Bean
    public OpenAPI apiCatedra() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de ms-catedra")
                        .version("1.0.0")
                        .description("Estructura académica, personas y equipos de la cátedra"))
                .addServersItem(new Server()
                        .url("/")
                        .description("Servidor actual"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
