package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ManejadorNoAutenticado implements ServerAuthenticationEntryPoint {

    private final EscritorProblemDetail escritor;

    public ManejadorNoAutenticado(EscritorProblemDetail escritor) {
        this.escritor = escritor;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange intercambio, AuthenticationException excepcion) {
        return escritor.escribir(intercambio, HttpStatus.UNAUTHORIZED,
                "No autorizado", "Se requiere un token válido");
    }
}
