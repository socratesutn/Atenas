package ar.edu.utn.frc.atenas.commons.http;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.traza.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterceptorHttpAtenasTest {

    private final InterceptorHttpAtenas interceptor = new InterceptorHttpAtenas();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("Propaga el JWT del usuario y el correlation-id en la llamada saliente")
    void deberiaPropagarJwtYCorrelationId_cuandoHayUsuarioAutenticado() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token-jwt")
                .header("alg", "none")
                .subject("usuario-1")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MDC.put(CorrelationIdFilter.NOMBRE_MDC, "traza-123");

        MockClientHttpRequest solicitud = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://localhost/recurso"));

        interceptor.intercept(solicitud, new byte[0], (pedido, cuerpo) -> {
            assertThat(pedido.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer token-jwt");
            assertThat(pedido.getHeaders().getFirst(CorrelationIdFilter.CABECERA))
                    .isEqualTo("traza-123");
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });
    }

    @Test
    @DisplayName("No propaga cabeceras de seguridad cuando no hay usuario autenticado")
    void deberiaOmitirCabeceras_cuandoNoHayUsuario() throws Exception {
        MockClientHttpRequest solicitud = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://localhost/recurso"));

        interceptor.intercept(solicitud, new byte[0], (pedido, cuerpo) -> {
            assertThat(pedido.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
            assertThat(pedido.getHeaders().getFirst(CorrelationIdFilter.CABECERA)).isNull();
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });
    }

    @Test
    @DisplayName("Traduce un timeout de conexión a ServicioNoDisponibleException")
    void deberiaTraducirTimeout_cuandoLaConexionNoResponde() {
        MockClientHttpRequest solicitud = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://localhost/recurso"));

        assertThatThrownBy(() -> interceptor.intercept(solicitud, new byte[0], (pedido, cuerpo) -> {
            throw new SocketTimeoutException("timeout");
        })).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Traduce una falla de acceso al recurso a ServicioNoDisponibleException")
    void deberiaTraducirResourceAccessException_cuandoLaDependenciaNoResponde() {
        MockClientHttpRequest solicitud = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://localhost/recurso"));

        assertThatThrownBy(() -> interceptor.intercept(solicitud, new byte[0], (pedido, cuerpo) -> {
            throw new ResourceAccessException("sin conexión", new SocketTimeoutException("timeout"));
        })).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
