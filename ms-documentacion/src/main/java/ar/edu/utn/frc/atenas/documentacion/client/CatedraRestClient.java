package ar.edu.utn.frc.atenas.documentacion.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.documentacion.config.CatedraProperties;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.CursoLectivoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.DocenteAsignadoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link CatedraClient} con {@link RestClient}.
 *
 * Usa el builder de atenas-commons, que ya trae timeouts, propagación del JWT
 * del usuario y del correlation-id, y traducción de errores HTTP. Se clona el
 * builder antes de fijar la URL base para no modificar el builder compartido
 * por otros clientes del contexto.
 */
@Component
public class CatedraRestClient implements CatedraClient {

    private static final String SERVICIO = "ms-catedra";

    private final RestClient restClient;

    @Autowired
    public CatedraRestClient(RestClient.Builder builder, CatedraProperties propiedades) {
        this(builder.clone().baseUrl(propiedades.getUrl()).build());
    }

    /** Constructor para tests: permite inyectar un cliente contra un servidor simulado. */
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
    public List<EquipoDto> listarEquipos(Long comisionLectivaId, Long cursoLectivoId) {
        if ((comisionLectivaId == null) == (cursoLectivoId == null)) {
            throw new SolicitudInvalidaException(
                    "Se debe indicar exactamente comisionLectivaId o cursoLectivoId");
        }

        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri(uriEquipos(comisionLectivaId, cursoLectivoId))
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
    public boolean docenteAsignado(Long comisionLectivaId, String sub) {
        DocenteAsignadoDto respuesta = LectorRespuestaInterna.leer(() -> restClient.get()
                .uri("/internal/comisiones-lectivas/{id}/docentes/{sub}", comisionLectivaId, sub)
                .retrieve()
                .body(DocenteAsignadoDto.class), SERVICIO);

        // Si el proveedor no informa el campo, no se asume permiso: se trata
        // como una respuesta incompleta del contrato.
        if (respuesta.asignado() == null) {
            throw new ServicioNoDisponibleException(
                    "El servicio " + SERVICIO + " devolvió una respuesta incompleta");
        }

        return respuesta.asignado();
    }

    @Override
    public CursoLectivoDto obtenerCurso(Long cursoLectivoId) {
        return LectorRespuestaInterna.leer(() -> restClient.get()
                .uri("/internal/cursos-lectivos/{id}", cursoLectivoId)
                .retrieve()
                .body(CursoLectivoDto.class), SERVICIO);
    }

    /**
     * Construye la URI de I02 con exactamente uno de los dos filtros.
     */
    private URI uriEquipos(Long comisionLectivaId, Long cursoLectivoId) {
        return UriComponentsBuilder.fromPath("/internal/equipos")
                .queryParamIfPresent("comisionLectivaId", Optional.ofNullable(comisionLectivaId))
                .queryParamIfPresent("cursoLectivoId", Optional.ofNullable(cursoLectivoId))
                .build()
                .toUri();
    }
}
