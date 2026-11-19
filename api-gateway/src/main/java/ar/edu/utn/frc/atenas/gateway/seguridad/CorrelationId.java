package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.UUID;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    private CorrelationId() {
    }

    public static String obtenerOGenerar(ServerHttpRequest solicitud) {
        String valor = solicitud.getHeaders().getFirst(HEADER);
        if (valor == null || valor.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return valor.trim();
    }
}
