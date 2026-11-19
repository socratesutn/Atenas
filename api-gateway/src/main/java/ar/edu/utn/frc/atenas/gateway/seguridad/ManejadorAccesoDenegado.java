package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ManejadorAccesoDenegado implements ServerAccessDeniedHandler {

    private final EscritorProblemDetail escritor;

    public ManejadorAccesoDenegado(EscritorProblemDetail escritor) {
        this.escritor = escritor;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange intercambio, AccessDeniedException excepcion) {
        return escritor.escribir(intercambio, HttpStatus.FORBIDDEN,
                "Acceso denegado", "El rol del token no permite realizar esta operación");
    }
}
