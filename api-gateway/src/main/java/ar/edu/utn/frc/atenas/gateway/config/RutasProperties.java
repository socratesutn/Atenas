package ar.edu.utn.frc.atenas.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atenas.rutas")
public record RutasProperties(
        String catedra,
        String documentacion,
        String seguimiento
) {
}
