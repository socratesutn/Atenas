package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.seguimiento.config.ClientesProperties;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.DocenteAsignadoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Implementación de {@link CatedraClient} sobre {@link RestClient}.
 *
 * <p>Usa el builder de {@code atenas-commons}: ya trae timeouts, propagación del JWT del
 * usuario y del correlation-id, y la traducción de errores HTTP. Se lo <b>clona</b> antes de
 * fijar la URL base, porque el builder del contexto se comparte con los demás clientes y
 * modificarlo directamente cambiaría también sus URLs.</p>
 */
@Component
public class CatedraRestClient implements CatedraClient {

    private static final String SERVICIO = "ms-catedra";

    private final RestClient restClient;

    @Autowired
    public CatedraRestClient(RestClient.Builder builder, ClientesProperties propiedades) {
        this(builder.clone().baseUrl(propiedades.getCatedra().getUrl()).build());
    }

    /** Constructor para tests: permite inyectar un cliente conectado a un servidor simulado. */
    CatedraRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public EquipoDto obtenerEquipo(Long equipoId) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri("/internal/equipos/{id}", equipoId)
                .retrieve()
                .body(EquipoDto.class), SERVICIO);
    }

    @Override
    public List<EquipoDto> listarEquiposPorComision(Long comisionLectivaId) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri(uri -> uri.path("/internal/equipos")
                        .queryParam("comisionLectivaId", "{id}").build(comisionLectivaId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<EquipoDto>>() {
                }), SERVICIO);
    }

    @Override
    public List<EquipoDto> listarEquiposPorCurso(Long cursoLectivoId) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri(uri -> uri.path("/internal/equipos")
                        .queryParam("cursoLectivoId", "{id}").build(cursoLectivoId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<EquipoDto>>() {
                }), SERVICIO);
    }

    @Override
    public EstudianteDto obtenerEstudiantePorSub(String sub) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri("/internal/estudiantes/por-sub/{sub}", sub)
                .retrieve()
                .body(EstudianteDto.class), SERVICIO);
    }

    @Override
    public boolean docenteAsignado(Long comisionLectivaId, String docenteSub) {
        DocenteAsignadoDto respuesta = LectorRespuestaInterna.leer(() -> restClient.get()
                .uri("/internal/comisiones-lectivas/{id}/docentes/{sub}", comisionLectivaId, docenteSub)
                .retrieve()
                .body(DocenteAsignadoDto.class), SERVICIO);
        // Si el campo falta no se asume que el docente está asignado: se trata como falla del contrato.
        if (respuesta.asignado() == null) {
            throw new ServicioNoDisponibleException(
                    "El servicio " + SERVICIO + " devolvió una respuesta incompleta");
        }
        return respuesta.asignado();
    }
}
