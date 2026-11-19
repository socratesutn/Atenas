package ar.edu.utn.frc.atenas.commons.traza;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    @DisplayName("Genera un X-Correlation-Id cuando la solicitud no lo trae")
    void deberiaGenerarCorrelationId_cuandoLaSolicitudNoLoTiene() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/prueba");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicReference<String> correlationIdEnLaCadena = new AtomicReference<>();

        FilterChain cadena = (req, res) ->
                correlationIdEnLaCadena.set(MDC.get(CorrelationIdFilter.NOMBRE_MDC));

        filtro.doFilter(solicitud, respuesta, cadena);

        String generado = respuesta.getHeader(CorrelationIdFilter.CABECERA);
        assertThat(generado).isNotBlank();
        assertThat(UUID.fromString(generado)).isNotNull();
        assertThat(correlationIdEnLaCadena.get()).isEqualTo(generado);
        assertThat(MDC.get(CorrelationIdFilter.NOMBRE_MDC)).isNull();
    }

    @Test
    @DisplayName("Respeta el X-Correlation-Id recibido y lo devuelve en la respuesta")
    void deberiaRespetarCorrelationId_cuandoLaSolicitudLoTiene() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/prueba");
        solicitud.addHeader(CorrelationIdFilter.CABECERA, "traza-recibida");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicReference<String> correlationIdEnLaCadena = new AtomicReference<>();

        FilterChain cadena = (req, res) ->
                correlationIdEnLaCadena.set(MDC.get(CorrelationIdFilter.NOMBRE_MDC));

        filtro.doFilter(solicitud, respuesta, cadena);

        assertThat(respuesta.getHeader(CorrelationIdFilter.CABECERA)).isEqualTo("traza-recibida");
        assertThat(correlationIdEnLaCadena.get()).isEqualTo("traza-recibida");
        assertThat(MDC.get(CorrelationIdFilter.NOMBRE_MDC)).isNull();
    }

    @Test
    @DisplayName("Genera un identificador nuevo cuando la cabecera recibida está vacía")
    void deberiaGenerarCorrelationId_cuandoLaCabeceraEstaVacia() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/prueba");
        solicitud.addHeader(CorrelationIdFilter.CABECERA, "   ");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        filtro.doFilter(solicitud, respuesta, (req, res) -> {
        });

        assertThat(respuesta.getHeader(CorrelationIdFilter.CABECERA))
                .isNotBlank()
                .isNotEqualTo("   ");
    }
}
