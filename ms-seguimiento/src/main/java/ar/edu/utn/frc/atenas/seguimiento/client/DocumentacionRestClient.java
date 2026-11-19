package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.seguimiento.config.ClientesProperties;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.HitoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link DocumentacionClient} sobre {@link RestClient}.
 * Ver {@link CatedraRestClient} para las decisiones sobre el builder compartido.
 */
@Component
public class DocumentacionRestClient implements DocumentacionClient {

    private static final String SERVICIO = "ms-documentacion";

    private final RestClient restClient;

    @Autowired
    public DocumentacionRestClient(RestClient.Builder builder, ClientesProperties propiedades) {
        this(builder.clone().baseUrl(propiedades.getDocumentacion().getUrl()).build());
    }

    /** Constructor para tests: permite inyectar un cliente conectado a un servidor simulado. */
    DocumentacionRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<HitoDto> listarHitosSprint(Long cursoLectivoId) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri(uri -> uri.path("/internal/hitos")
                        .queryParam("cursoLectivoId", "{cursoLectivoId}")
                        .queryParam("tipo", "SPRINT")
                        .build(cursoLectivoId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<HitoDto>>() {
                }), SERVICIO);
    }

    @Override
    public List<EntregaEquipoDto> listarEntregas(Long cursoLectivoId, List<Long> equipoIds) {
        // Los ids viajan separados por coma: equipoIds=1,2
        String ids = equipoIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri(uri -> uri.path("/internal/entregas")
                        .queryParam("cursoLectivoId", "{cursoLectivoId}")
                        .queryParam("equipoIds", "{equipoIds}")
                        .build(cursoLectivoId, ids))
                .retrieve()
                .body(new ParameterizedTypeReference<List<EntregaEquipoDto>>() {
                }), SERVICIO);
    }
}
