package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.SecurityConfig;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.service.EventoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S01 y S02: códigos HTTP, validación 400, 401, 403 por rol y forma de los errores. */
@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ZonaHoraria.class})
class EventoControllerTest {

    private static final String RUTA = "/api/seguimiento/comisiones-lectivas/10/eventos";
    private static final String CUERPO_VALIDO =
            "{\"tipo\":\"CLASE\",\"fecha\":\"2026-11-03T09:00:00-03:00\",\"descripcion\":\"Repaso\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static SimpleGrantedAuthority rol(String nombre) {
        return new SimpleGrantedAuthority("ROLE_" + nombre);
    }

    private static Evento evento() {
        return Evento.builder().id(1L).comisionLectivaId(10L).tipo(TipoEvento.CLASE)
                .fecha(Instant.parse("2026-11-03T12:00:00Z")).descripcion("Repaso").build();
    }

    // ---------------------------------------------------------------- S01

    @Test
    @DisplayName("RN-12: un docente crea un evento y recibe 201 con los datos del evento")
    void deberiaResponder201_cuandoElDocenteCreaUnEvento() throws Exception {
        when(servicio.crear(eq(10L), eq(TipoEvento.CLASE), eq(Instant.parse("2026-11-03T12:00:00Z")), eq("Repaso")))
                .thenReturn(evento());

        mockMvc.perform(post(RUTA).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.comisionLectivaId").value(10))
                .andExpect(jsonPath("$.tipo").value("CLASE"))
                .andExpect(jsonPath("$.descripcion").value("Repaso"))
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    @DisplayName("S01 sin token responde 401")
    void deberiaResponder401_cuandoNoHayToken() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "el rol {0} recibe 403 al crear un evento")
    @ValueSource(strings = {"ALUMNO", "JEFE_CATEDRA"})
    @DisplayName("RN-12: un rol distinto de DOCENTE no crea eventos")
    void deberiaResponder403_cuandoElRolNoEsDocente(String rol) throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().authorities(rol(rol)))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "cuerpo inválido: {0}")
    @ValueSource(strings = {
            "{\"tipo\":\"EXAMEN\",\"fecha\":\"2026-11-03T09:00:00-03:00\"}",
            "{\"tipo\":\"\",\"fecha\":\"2026-11-03T09:00:00-03:00\"}",
            "{\"fecha\":\"2026-11-03T09:00:00-03:00\"}",
            "{\"tipo\":\"CLASE\"}"})
    @DisplayName("RN-12: un tipo distinto de CLASE o PRESENTACION, o la falta de tipo o fecha, responde 400")
    void deberiaResponder400_cuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(servicio, never()).crear(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("RN-12: un docente no asignado a la comisión recibe 403 con la regla")
    void deberiaResponder403ConLaRegla_cuandoElDocenteNoEstaAsignado() throws Exception {
        when(servicio.crear(anyLong(), any(), any(), any()))
                .thenThrow(new AccesoDenegadoException("No asignado", "RN-12"));

        mockMvc.perform(post(RUTA).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-12"));
    }

    @Test
    @DisplayName("Una comisión inexistente responde 404")
    void deberiaResponder404_cuandoLaComisionNoExiste() throws Exception {
        when(servicio.crear(anyLong(), any(), any(), any())).thenThrow(new NoEncontradoException("No existe la comisión"));

        mockMvc.perform(post(RUTA).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RN-12: si ms-catedra no responde, 503")
    void deberiaResponder503_cuandoMsCatedraNoResponde() throws Exception {
        when(servicio.crear(anyLong(), any(), any(), any()))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        mockMvc.perform(post(RUTA).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------------------------------------------------------------- S02

    @ParameterizedTest(name = "el rol {0} consulta el calendario")
    @ValueSource(strings = {"JEFE_CATEDRA", "DOCENTE", "ALUMNO"})
    @DisplayName("RN-01: el jefe, el docente y el alumno consultan el calendario y reciben 200")
    void deberiaResponder200_cuandoSeConsultaElCalendario(String rol) throws Exception {
        when(servicio.listar(10L)).thenReturn(List.of(evento()));

        mockMvc.perform(get(RUTA).with(jwt().authorities(rol(rol))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipo").value("CLASE"));
    }

    @Test
    @DisplayName("S02 sin token responde 401")
    void deberiaResponder401_alConsultarSinToken() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("RN-01: quien no pertenece a la comisión recibe 403")
    void deberiaResponder403_cuandoNoPerteneceALaComision() throws Exception {
        when(servicio.listar(10L)).thenThrow(new AccesoDenegadoException("Sin acceso", "RN-01"));

        mockMvc.perform(get(RUTA).with(jwt().authorities(rol("ALUMNO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-01"));
    }
}
