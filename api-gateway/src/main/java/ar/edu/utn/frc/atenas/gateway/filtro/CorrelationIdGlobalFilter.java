package ar.edu.utn.frc.atenas.gateway.filtro;

import ar.edu.utn.frc.atenas.gateway.seguridad.CorrelationId;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, GatewayFilterChain cadena) {
        String correlationId = CorrelationId.obtenerOGenerar(intercambio.getRequest());

        ServerHttpRequest solicitud = intercambio.getRequest().mutate()
                .headers(headers -> headers.set(CorrelationId.HEADER, correlationId))
                .build();

        intercambio.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId);

        return cadena.filter(intercambio.mutate().request(solicitud).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
