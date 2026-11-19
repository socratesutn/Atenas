package ar.edu.utn.frc.atenas.commons.error;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.commons.traza.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManejadorGlobalDeErroresTest {

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ZonaHoraria zonaHoraria = new ZonaHoraria(ZonaHoraria.ZONA_POR_DEFECTO);
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalDeErrores(zonaHoraria))
                .build();
        MDC.clear();
    }

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    static Stream<Arguments> excepcionesDeDominio() {
        return Stream.of(
                Arguments.of(400, "Solicitud inválida", "Mensaje 400"),
                Arguments.of(403, "Acceso denegado", "Mensaje 403"),
                Arguments.of(404, "No encontrado", "Mensaje 404"),
                Arguments.of(409, "Conflicto", "Mensaje 409"),
                Arguments.of(422, "Entidad no procesable", "Mensaje 422"),
                Arguments.of(503, "Servicio no disponible", "Mensaje 503"));
    }

    @ParameterizedTest(name = "{0} devuelve ''{1}''")
    @MethodSource("excepcionesDeDominio")
    @DisplayName("Cada excepción de dominio produce su status y los campos del ProblemDetail")
    void deberiaProducirStatusYTitulo_cuandoSeLanzaUnaExcepcionDeDominio(int codigo, String titulo,
                                                                          String detalle) throws Exception {
        MDC.put(CorrelationIdFilter.NOMBRE_MDC, "traza-123");

        mockMvc.perform(get("/prueba/excepcion/{codigo}", codigo))
                .andExpect(status().is(codigo))
                .andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.title").value(titulo))
                .andExpect(jsonPath("$.detail").value(detalle))
                .andExpect(jsonPath("$.instance").value("/prueba/excepcion/" + codigo))
                .andExpect(jsonPath("$.timestamp").value(endsWith("-03:00")))
                .andExpect(jsonPath("$.regla").value("RN-99"))
                .andExpect(jsonPath("$.correlationId").value("traza-123"));
    }

    @Test
    @DisplayName("Omite el campo regla cuando la excepción no tiene identificador de regla")
    void deberiaOmitirRegla_cuandoNoTieneIdentificadorDeRegla() throws Exception {
        mockMvc.perform(get("/prueba/sin-regla"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("No encontrado"))
                .andExpect(jsonPath("$.regla").doesNotExist())
                .andExpect(jsonPath("$.correlationId").doesNotExist());
    }

    @Test
    @DisplayName("400: informa los errores de validación con campo y mensaje")
    void deberiaInformarErroresDeValidacion_cuandoElRequestEsInvalido() throws Exception {
        MDC.put(CorrelationIdFilter.NOMBRE_MDC, "traza-validacion");

        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value("La solicitud contiene datos inválidos"))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("no debe estar vacío"))
                .andExpect(jsonPath("$.correlationId").value("traza-validacion"));
    }

    @Test
    @DisplayName("403: un AccessDeniedException se responde con ProblemDetail")
    void deberiaResponder403_cuandoSeDeniegaElAcceso() throws Exception {
        mockMvc.perform(get("/prueba/acceso-denegado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.detail").value("Sin permiso"));
    }

    @Test
    @DisplayName("500: no expone detalles internos ante una excepción no prevista")
    void deberiaOcultarDetallesInternos_cuandoLaExcepcionNoEstaPrevista() throws Exception {
        mockMvc.perform(get("/prueba/excepcion/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                .andExpect(jsonPath("$.detail").value("Se produjo un error inesperado"))
                .andExpect(jsonPath("$.regla").doesNotExist());
    }

    @RestController
    @RequestMapping("/prueba")
    static class ControladorDePrueba {

        @GetMapping("/excepcion/{codigo}")
        void lanzar(@PathVariable int codigo) {
            switch (codigo) {
                case 400 -> throw new SolicitudInvalidaException("Mensaje 400", "RN-99");
                case 403 -> throw new AccesoDenegadoException("Mensaje 403", "RN-99");
                case 404 -> throw new NoEncontradoException("Mensaje 404", "RN-99");
                case 409 -> throw new ConflictoException("Mensaje 409", "RN-99");
                case 422 -> throw new NoProcesableException("Mensaje 422", "RN-99");
                case 503 -> throw new ServicioNoDisponibleException("Mensaje 503", "RN-99");
                default -> throw new IllegalStateException("detalle interno que no debe exponerse");
            }
        }

        @GetMapping("/sin-regla")
        void sinRegla() {
            throw new NoEncontradoException("No existe");
        }

        @GetMapping("/acceso-denegado")
        void accesoDenegado() {
            throw new AccessDeniedException("Sin permiso");
        }

        @PostMapping("/validacion")
        void validar(@Valid @RequestBody Pedido pedido) {
        }

        record Pedido(@NotBlank(message = "no debe estar vacío") String nombre) {
        }
    }
}
