package ar.edu.utn.frc.atenas.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RutasConfig {

    @Bean
    public RouteLocator rutas(RouteLocatorBuilder constructor, RutasProperties propiedades) {
        return constructor.routes()
                .route("catedra", ruta -> ruta
                        .path("/api/catedra/**")
                        .uri(propiedades.catedra()))
                .route("documentacion", ruta -> ruta
                        .path("/api/documentacion/**")
                        .uri(propiedades.documentacion()))
                .route("seguimiento", ruta -> ruta
                        .path("/api/seguimiento/**")
                        .uri(propiedades.seguimiento()))
                .build();
    }
}
