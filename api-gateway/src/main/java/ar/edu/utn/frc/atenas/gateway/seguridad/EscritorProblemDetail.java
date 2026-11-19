package ar.edu.utn.frc.atenas.gateway.seguridad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class EscritorProblemDetail {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Cordoba");

    private final ObjectMapper objectMapper;

    public EscritorProblemDetail(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> escribir(ServerWebExchange intercambio, HttpStatus estado,
                               String titulo, String detalle) {
        String correlationId = CorrelationId.obtenerOGenerar(intercambio.getRequest());

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(intercambio.getRequest().getPath().value()));
        problema.setProperty("timestamp", OffsetDateTime.now(ZONA_HORARIA).toString());
        problema.setProperty("correlationId", correlationId);

        intercambio.getResponse().setStatusCode(estado);
        intercambio.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        intercambio.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId);

        byte[] contenido;
        try {
            contenido = objectMapper.writeValueAsBytes(problema);
        } catch (JsonProcessingException excepcion) {
            return Mono.error(excepcion);
        }

        DataBuffer buffer = intercambio.getResponse().bufferFactory().wrap(contenido);
        return intercambio.getResponse().writeWith(Mono.just(buffer));
    }
}
