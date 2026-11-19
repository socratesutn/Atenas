package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import ar.edu.utn.frc.atenas.commons.http.ConfiguracionHttp;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.HitoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de ms-documentacion con un servidor simulado (sin red). */
class DocumentacionRestClientTest {

    private static final String HITOS_JSON = """
            [{"id":3,"cursoLectivoId":2,"nombre":"Sprint 1","tipo":"SPRINT","orden":1,
              "fechaInicio":"2026-11-02T00:00:00-03:00","fechaLimite":"2026-11-09T23:59:59-03:00","extra":true},
             {"id":4,"cursoLectivoId":2,"nombre":"Sprint 2","tipo":"SPRINT","orden":2,
              "fechaInicio":"2026-11-10T03:00:00Z","fechaLimite":"2026-11-17T03:00:00Z"}]""";

    private MockRestServiceServer servidor;
    private DocumentacionRestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = new ConfiguracionHttp()
                .restClientBuilder(new AtenasHttpProperties())
                .baseUrl("http://ms-documentacion:8080");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new DocumentacionRestClient(builder.build());
    }

    private static RequestMatcher pedidoA(String ruta, String... fragmentosDeConsulta) {
        return solicitud -> {
            URI uri = solicitud.getURI();
            assertThat(uri.getRawPath()).isEqualTo(ruta);
            for (String fragmento : fragmentosDeConsulta) {
                assertThat(uri.getQuery()).contains(fragmento);
            }
        };
    }

    // ---------------------------------------------------------------- I06

    @Test
    @DisplayName("I06: lista los hitos SPRINT de un curso con sus fechas, aunque vengan con distinto offset")
    void deberiaListarLosHitosSprint_deUnCurso() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/hitos", "cursoLectivoId=2", "tipo=SPRINT"))
                .andRespond(withSuccess(HITOS_JSON, MediaType.APPLICATION_JSON));

        List<HitoDto> hitos = cliente.listarHitosSprint(2L);

        assertThat(hitos).extracting(HitoDto::id).containsExactly(3L, 4L);
        assertThat(hitos.get(0).tipo()).isEqualTo("SPRINT");
        assertThat(hitos.get(0).fechaInicio().toInstant()).isEqualTo(Instant.parse("2026-11-02T03:00:00Z"));
        assertThat(hitos.get(0).fechaLimite().toInstant()).isEqualTo(Instant.parse("2026-11-10T02:59:59Z"));
        assertThat(hitos.get(1).fechaInicio().toInstant()).isEqualTo(Instant.parse("2026-11-10T03:00:00Z"));
        servidor.verify();
    }

    @Test
    @DisplayName("I06: un curso sin sprints devuelve una lista vacía")
    void deberiaDevolverListaVacia_cuandoNoHaySprints() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(cliente.listarHitosSprint(2L)).isEmpty();
    }

    @Test
    @DisplayName("I06: un curso inexistente (404) se traduce a NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoMsDocumentacionResponde404() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> cliente.listarHitosSprint(2L));
    }

    @ParameterizedTest(name = "el estado {0} se traduce a ServicioNoDisponibleException")
    @ValueSource(ints = {500, 502, 503, 400, 401, 403})
    @DisplayName("Un error 5xx u otro 4xx inesperado se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoMsDocumentacionRespondeError(int estado) {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.valueOf(estado)));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarHitosSprint(2L));
    }

    @Test
    @DisplayName("Un timeout se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoVenceElTiempo() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarHitosSprint(2L));
    }

    @Test
    @DisplayName("Un cuerpo que no es una lista de hitos se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoElCuerpoNoSePuedeInterpretar() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"mensaje\":\"otra cosa\"}", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarHitosSprint(2L));
    }

    // ---------------------------------------------------------------- I07

    @Test
    @DisplayName("I07: pide las entregas con los ids de equipo separados por coma y devuelve estado y nota")
    void deberiaListarLasEntregas_deLosEquipos() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/entregas", "cursoLectivoId=2", "equipoIds=7,8"))
                .andRespond(withSuccess("""
                        [{"equipoId":7,"hitoId":3,"estado":"CORREGIDA","tardia":false,"nota":8},
                         {"equipoId":7,"hitoId":4,"estado":"PENDIENTE","tardia":null,"nota":null},
                         {"equipoId":8,"hitoId":3,"estado":"ENTREGADA","tardia":true,"nota":null,"extra":"x"}]""",
                        MediaType.APPLICATION_JSON));

        List<EntregaEquipoDto> entregas = cliente.listarEntregas(2L, List.of(7L, 8L));

        assertThat(entregas).hasSize(3);
        assertThat(entregas.get(0)).isEqualTo(new EntregaEquipoDto(7L, 3L, "CORREGIDA", false, 8));
        assertThat(entregas.get(1).estado()).isEqualTo("PENDIENTE");
        assertThat(entregas.get(1).nota()).isNull();
        assertThat(entregas.get(2).tardia()).isTrue();
        servidor.verify();
    }

    @Test
    @DisplayName("I07: un error de ms-documentacion al listar entregas es ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoFallaElListadoDeEntregas() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarEntregas(2L, List.of(7L)));
    }
}
