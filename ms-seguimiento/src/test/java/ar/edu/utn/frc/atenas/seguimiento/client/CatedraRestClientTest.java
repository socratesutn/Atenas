package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import ar.edu.utn.frc.atenas.commons.http.ConfiguracionHttp;
import ar.edu.utn.frc.atenas.commons.traza.CorrelationIdFilter;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de ms-catedra con un servidor simulado (sin red). */
class CatedraRestClientTest {

    private static final String EQUIPO_JSON = """
            {"id":7,"nombre":"Equipo 1","comisionLectivaId":10,"cursoLectivoId":2,
             "estado":"ASEGURADO","asegurado":true,"representanteSub":"sub-rep","campoNuevo":"se ignora",
             "integrantes":[{"estudianteId":5,"sub":"sub-rep","legajo":"411626","usuarioGithub":"usuarioa","otro":1},
                            {"estudianteId":6,"sub":"sub-b","legajo":"411627","usuarioGithub":null}]}""";

    private MockRestServiceServer servidor;
    private CatedraRestClient cliente;

    @BeforeEach
    void preparar() {
        // El mismo builder de commons que usa la aplicación: trae interceptor y manejo de errores.
        RestClient.Builder builder = new ConfiguracionHttp()
                .restClientBuilder(new AtenasHttpProperties())
                .baseUrl("http://ms-catedra:8080");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new CatedraRestClient(builder.build());
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        MDC.clear();
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

    // ---------------------------------------------------------------- I01

    @Test
    @DisplayName("I01: devuelve el equipo con su representante e integrantes e ignora propiedades desconocidas")
    void deberiaDevolverElEquipo_cuandoMsCatedraResponde200() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/equipos/7"))
                .andRespond(withSuccess(EQUIPO_JSON, MediaType.APPLICATION_JSON));

        EquipoDto equipo = cliente.obtenerEquipo(7L);

        assertThat(equipo.id()).isEqualTo(7L);
        assertThat(equipo.comisionLectivaId()).isEqualTo(10L);
        assertThat(equipo.cursoLectivoId()).isEqualTo(2L);
        assertThat(equipo.estado()).isEqualTo("ASEGURADO");
        assertThat(equipo.asegurado()).isTrue();
        assertThat(equipo.representanteSub()).isEqualTo("sub-rep");
        assertThat(equipo.integrantes()).extracting(EquipoDto.IntegranteDto::usuarioGithub)
                .containsExactly("usuarioa", null);
        servidor.verify();
    }

    @Test
    @DisplayName("I01: un equipo inexistente (404) se traduce a NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoElEquipoNoExiste() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> cliente.obtenerEquipo(7L));
    }

    @ParameterizedTest(name = "el estado {0} se traduce a ServicioNoDisponibleException")
    @ValueSource(ints = {500, 502, 503, 400, 401, 403})
    @DisplayName("Un error 5xx u otro 4xx inesperado se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoMsCatedraRespondeError(int estado) {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.valueOf(estado)));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerEquipo(7L));
    }

    @Test
    @DisplayName("Un timeout o una falla de conexión se traducen a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoVenceElTiempo() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerEquipo(7L));
    }

    @Test
    @DisplayName("Un cuerpo que no es el contrato esperado se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoElCuerpoNoSePuedeInterpretar() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("[1,2,3]", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerEquipo(7L));
    }

    @Test
    @DisplayName("Una respuesta 200 sin contenido se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoLaRespuestaVieneVacia() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerEquipo(7L));
    }

    // ---------------------------------------------------------------- I02

    @Test
    @DisplayName("I02: lista los equipos de una comisión lectiva")
    void deberiaListarLosEquipos_deUnaComision() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/equipos", "comisionLectivaId=10"))
                .andRespond(withSuccess("[" + EQUIPO_JSON + "," + EQUIPO_JSON.replace("\"id\":7", "\"id\":8") + "]",
                        MediaType.APPLICATION_JSON));

        List<EquipoDto> equipos = cliente.listarEquiposPorComision(10L);

        assertThat(equipos).extracting(EquipoDto::id).containsExactly(7L, 8L);
    }

    @Test
    @DisplayName("I02: lista los equipos de un curso lectivo")
    void deberiaListarLosEquipos_deUnCurso() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/equipos", "cursoLectivoId=2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(cliente.listarEquiposPorCurso(2L)).isEmpty();
    }

    @Test
    @DisplayName("I02: un error de ms-catedra al listar equipos es ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoFallaElListadoDeEquipos() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarEquiposPorComision(10L));
    }

    // ---------------------------------------------------------------- I03

    @Test
    @DisplayName("I03: devuelve la comisión, el curso y el equipo vigentes; el equipo puede ser nulo")
    void deberiaDevolverElEstudiante_cuandoNoTieneEquipo() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/estudiantes/por-sub/uuid-1"))
                .andRespond(withSuccess("{\"estudianteId\":5,\"sub\":\"uuid-1\",\"activo\":true,"
                        + "\"comisionLectivaId\":10,\"cursoLectivoId\":2,\"equipoId\":null}", MediaType.APPLICATION_JSON));

        EstudianteDto estudiante = cliente.obtenerEstudiantePorSub("uuid-1");

        assertThat(estudiante.comisionLectivaId()).isEqualTo(10L);
        assertThat(estudiante.cursoLectivoId()).isEqualTo(2L);
        assertThat(estudiante.equipoId()).isNull();
    }

    @Test
    @DisplayName("I03: un alumno inexistente (404) se traduce a NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoElAlumnoNoExiste() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> cliente.obtenerEstudiantePorSub("uuid-1"));
    }

    // ---------------------------------------------------------------- I04

    @Test
    @DisplayName("I04: informa que el docente está asignado a la comisión")
    void deberiaDevolverVerdadero_cuandoElDocenteEstaAsignado() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/internal/comisiones-lectivas/10/docentes/uuid-doc"))
                .andRespond(withSuccess("{\"asignado\":true}", MediaType.APPLICATION_JSON));

        assertThat(cliente.docenteAsignado(10L, "uuid-doc")).isTrue();
    }

    @Test
    @DisplayName("I04: informa que el docente no está asignado a la comisión")
    void deberiaDevolverFalso_cuandoElDocenteNoEstaAsignado() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"asignado\":false}", MediaType.APPLICATION_JSON));

        assertThat(cliente.docenteAsignado(10L, "uuid-doc")).isFalse();
    }

    @Test
    @DisplayName("I04: una comisión inexistente (404) se traduce a NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoLaComisionNoExiste() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> cliente.docenteAsignado(99L, "uuid-doc"));
    }

    @Test
    @DisplayName("I04: si falta el campo asignado no se asume permiso: ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoFaltaElCampoAsignado() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.docenteAsignado(10L, "uuid-doc"));
    }

    // ---------------------------------------------------------------- propagación

    @Test
    @DisplayName("Propaga el JWT del usuario y el correlation-id a ms-catedra")
    void deberiaPropagarElJwtYElCorrelationId() {
        Jwt jwt = Jwt.withTokenValue("token-del-usuario").header("alg", "none").subject("sub-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MDC.put(CorrelationIdFilter.NOMBRE_MDC, "corr-123");
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(solicitud -> {
                    assertThat(solicitud.getHeaders().getFirst("Authorization")).isEqualTo("Bearer token-del-usuario");
                    assertThat(solicitud.getHeaders().getFirst(CorrelationIdFilter.CABECERA)).isEqualTo("corr-123");
                })
                .andRespond(withSuccess(EQUIPO_JSON, MediaType.APPLICATION_JSON));

        cliente.obtenerEquipo(7L);

        servidor.verify();
    }

    @Test
    @DisplayName("Sin usuario autenticado no se manda la cabecera Authorization")
    void deberiaOmitirAuthorization_cuandoNoHayUsuario() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(solicitud -> assertThat(solicitud.getHeaders().containsKey("Authorization")).isFalse())
                .andRespond(withSuccess(EQUIPO_JSON, MediaType.APPLICATION_JSON));

        cliente.obtenerEquipo(7L);

        servidor.verify();
    }
}
