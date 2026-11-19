package ar.edu.utn.frc.atenas.gateway;

import ar.edu.utn.frc.atenas.gateway.seguridad.CorrelationId;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    private static final SimpleGrantedAuthority ROL_JEFE =
            new SimpleGrantedAuthority("ROLE_JEFE_CATEDRA");
    private static final SimpleGrantedAuthority ROL_ALUMNO =
            new SimpleGrantedAuthority("ROLE_ALUMNO");

    private static final DisposableServer BACKEND = HttpServer.create()
            .host("localhost")
            .port(0)
            .handle((solicitud, respuesta) -> {
                String correlationId = solicitud.requestHeaders().get(CorrelationId.HEADER);
                if (correlationId == null) {
                    correlationId = "";
                }
                String ruta = solicitud.path();
                if (!ruta.startsWith("/")) {
                    ruta = "/" + ruta;
                }
                String cuerpo = ruta + "|" + correlationId;
                return respuesta.status(HttpResponseStatus.OK)
                        .header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                        .sendString(Mono.just(cuerpo))
                        .then();
            })
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configurarRutas(DynamicPropertyRegistry registro) {
        String uri = "http://localhost:" + BACKEND.port();
        registro.add("atenas.rutas.catedra", () -> uri);
    }

    @AfterAll
    static void detenerBackend() {
        BACKEND.disposeNow();
    }

    @Test
    @DisplayName("GET /actuator/health responde 200 sin token")
    void deberiaResponderOk_cuandoSeConsultaHealthSinToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Un endpoint protegido responde 401 sin token en formato ProblemDetail")
    void deberiaResponderNoAutorizado_cuandoNoHayToken() {
        webTestClient.get()
                .uri("/api/catedra/docentes")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists(CorrelationId.HEADER)
                .expectBody()
                .jsonPath("$.title").isEqualTo("No autorizado")
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.instance").isEqualTo("/api/catedra/docentes");
    }

    @Test
    @DisplayName("Un rol incorrecto responde 403 en formato ProblemDetail")
    void deberiaResponderAccesoDenegado_cuandoElRolNoEstaPermitido() {
        webTestClient.mutateWith(mockJwt().authorities(ROL_ALUMNO))
                .get()
                .uri("/api/catedra/docentes")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Acceso denegado")
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    @DisplayName("Un rol permitido accede a la ruta y se conserva el prefijo")
    void deberiaPermitirElPaso_cuandoElRolEsCorrecto() {
        EntityExchangeResult<String> resultado = webTestClient
                .mutateWith(mockJwt().authorities(ROL_JEFE))
                .get()
                .uri("/api/catedra/docentes")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(CorrelationId.HEADER)
                .expectBody(String.class)
                .returnResult();

        String correlationId = resultado.getResponseHeaders().getFirst(CorrelationId.HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(resultado.getResponseBody())
                .isEqualTo("/api/catedra/docentes|" + correlationId);
    }

    @Test
    @DisplayName("Una ruta interna responde 404 con un token válido")
    void deberiaResponderNoEncontrado_cuandoSeConsultaUnaRutaInterna() {
        webTestClient.mutateWith(mockJwt().authorities(ROL_JEFE))
                .get()
                .uri("/internal/equipos/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("El correlation-id enviado se propaga y se devuelve")
    void deberiaConservarElCorrelationId_cuandoElClienteLoEnvia() {
        String correlationId = "prueba-correlacion";

        webTestClient.mutateWith(mockJwt().authorities(ROL_JEFE))
                .get()
                .uri("/api/catedra/docentes")
                .header(CorrelationId.HEADER, correlationId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationId.HEADER, correlationId)
                .expectBody(String.class)
                .isEqualTo("/api/catedra/docentes|" + correlationId);
    }

    @Test
    @DisplayName("La documentación OpenAPI pasa sin token")
    void deberiaPermitirLaDocumentacion_cuandoNoHayToken() {
        webTestClient.get()
                .uri("/api/catedra/v3/api-docs")
                .exchange()
                .expectStatus().isOk();
    }
}
