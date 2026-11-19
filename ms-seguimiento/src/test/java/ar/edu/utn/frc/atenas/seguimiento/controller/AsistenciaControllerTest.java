package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.SecurityConfig;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.service.AsistenciaService;
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S03 a S06: códigos HTTP, validación 400, 401, 403 por rol y forma de los errores. */
@WebMvcTest(AsistenciaController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ZonaHoraria.class})
class AsistenciaControllerTest {

    private static final String RUTA_ALUMNO = "/api/seguimiento/eventos/3/asistencias/sub-a";
    private static final String RUTA_EQUIPO = "/api/seguimiento/eventos/3/equipos/7/asistencias";
    private static final String RUTA_EVENTO = "/api/seguimiento/eventos/3/asistencias";
    private static final String RUTA_PROPIA = "/api/seguimiento/mi-asistencia";
    private static final String PRESENTE = "{\"presente\":true}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsistenciaService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static SimpleGrantedAuthority rol(String nombre) {
        return new SimpleGrantedAuthority("ROLE_" + nombre);
    }

    private static Evento evento() {
        return Evento.builder().id(3L).comisionLectivaId(10L).tipo(TipoEvento.CLASE)
                .fecha(Instant.parse("2026-11-03T12:00:00Z")).descripcion("Repaso").build();
    }

    private static Asistencia asistencia(String sub, boolean presente) {
        return Asistencia.builder().id(1L).evento(evento()).estudianteSub(sub).equipoId(7L).presente(presente)
                .registradaPorSub("sub-doc").fechaRegistro(Instant.parse("2026-11-04T10:00:00Z")).build();
    }

    // ---------------------------------------------------------------- S03

    @Test
    @DisplayName("RN-13: un docente registra la asistencia de un alumno y recibe 200")
    void deberiaResponder200_cuandoElDocenteRegistraLaAsistenciaDeUnAlumno() throws Exception {
        when(servicio.registrarDeAlumno(3L, "sub-a", true)).thenReturn(asistencia("sub-a", true));

        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value(3))
                .andExpect(jsonPath("$.estudianteSub").value("sub-a"))
                .andExpect(jsonPath("$.equipoId").value(7))
                .andExpect(jsonPath("$.presente").value(true))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());
    }

    @Test
    @DisplayName("S03 sin token responde 401")
    void deberiaResponder401_alRegistrarSinToken() throws Exception {
        mockMvc.perform(put(RUTA_ALUMNO).contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "el rol {0} recibe 403 al registrar asistencia")
    @ValueSource(strings = {"ALUMNO", "JEFE_CATEDRA"})
    @DisplayName("RN-13: un rol distinto de DOCENTE no registra asistencia")
    void deberiaResponder403_cuandoElRolNoEsDocente(String rol) throws Exception {
        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol(rol)))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(RUTA_EQUIPO).with(jwt().authorities(rol(rol)))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("RN-13: sin el campo presente responde 400 y no llega al servicio")
    void deberiaResponder400_cuandoFaltaPresente() throws Exception {
        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(servicio, never()).registrarDeAlumno(anyLong(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("RN-13: un evento futuro responde 409 con la regla")
    void deberiaResponder409_cuandoElEventoEsFuturo() throws Exception {
        when(servicio.registrarDeAlumno(3L, "sub-a", true))
                .thenThrow(new ConflictoException("Evento futuro", "RN-13"));

        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regla").value("RN-13"));
    }

    @Test
    @DisplayName("RN-13: un alumno o docente de otra comisión recibe 403")
    void deberiaResponder403_cuandoEsDeOtraComision() throws Exception {
        when(servicio.registrarDeAlumno(3L, "sub-a", true))
                .thenThrow(new AccesoDenegadoException("Otra comisión", "RN-13"));

        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-13"));
    }

    @Test
    @DisplayName("Un evento inexistente responde 404")
    void deberiaResponder404_cuandoElEventoNoExiste() throws Exception {
        when(servicio.registrarDeAlumno(3L, "sub-a", true)).thenThrow(new NoEncontradoException("No existe el evento"));

        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RN-13: si ms-catedra no responde, 503")
    void deberiaResponder503_cuandoMsCatedraNoResponde() throws Exception {
        when(servicio.registrarDeAlumno(3L, "sub-a", true))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        mockMvc.perform(put(RUTA_ALUMNO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------------------------------------------------------------- S04

    @Test
    @DisplayName("RN-13: un docente registra la asistencia de un equipo y recibe la lista de integrantes")
    void deberiaResponder200_cuandoElDocenteRegistraLaAsistenciaDeUnEquipo() throws Exception {
        when(servicio.registrarDeEquipo(3L, 7L, false))
                .thenReturn(List.of(asistencia("sub-a", false), asistencia("sub-b", false)));

        mockMvc.perform(put(RUTA_EQUIPO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"presente\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estudianteSub").value("sub-a"))
                .andExpect(jsonPath("$[1].presente").value(false));
    }

    @Test
    @DisplayName("RN-13: un equipo sin integrantes responde 409")
    void deberiaResponder409_cuandoElEquipoNoTieneIntegrantes() throws Exception {
        when(servicio.registrarDeEquipo(3L, 7L, true))
                .thenThrow(new ConflictoException("Sin integrantes", "RN-13"));

        mockMvc.perform(put(RUTA_EQUIPO).with(jwt().authorities(rol("DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- S05

    @ParameterizedTest(name = "el rol {0} consulta la asistencia de un evento")
    @ValueSource(strings = {"JEFE_CATEDRA", "DOCENTE"})
    @DisplayName("RN-01: el jefe y el docente consultan la asistencia de un evento")
    void deberiaResponder200_cuandoSeConsultaLaAsistenciaDeUnEvento(String rol) throws Exception {
        when(servicio.listarDeEvento(3L)).thenReturn(List.of(asistencia("sub-a", true)));

        mockMvc.perform(get(RUTA_EVENTO).with(jwt().authorities(rol(rol))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estudianteSub").value("sub-a"));
    }

    @Test
    @DisplayName("RN-01: un alumno no consulta la asistencia de un evento: 403")
    void deberiaResponder403_cuandoUnAlumnoConsultaLaAsistenciaDeUnEvento() throws Exception {
        mockMvc.perform(get(RUTA_EVENTO).with(jwt().authorities(rol("ALUMNO"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión recibe 403 al consultar la asistencia")
    void deberiaResponder403_cuandoElDocenteEsDeOtraComision() throws Exception {
        when(servicio.listarDeEvento(3L)).thenThrow(new AccesoDenegadoException("Otra comisión", "RN-01"));

        mockMvc.perform(get(RUTA_EVENTO).with(jwt().authorities(rol("DOCENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-01"));
    }

    // ---------------------------------------------------------------- S06

    @Test
    @DisplayName("RN-15: un alumno consulta su asistencia con los datos de cada evento")
    void deberiaResponder200_cuandoElAlumnoConsultaSuAsistencia() throws Exception {
        when(servicio.miAsistencia()).thenReturn(List.of(asistencia("sub-alumno", true)));

        mockMvc.perform(get(RUTA_PROPIA).with(jwt().authorities(rol("ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value(3))
                .andExpect(jsonPath("$[0].comisionLectivaId").value(10))
                .andExpect(jsonPath("$[0].tipo").value("CLASE"))
                .andExpect(jsonPath("$[0].descripcion").value("Repaso"))
                .andExpect(jsonPath("$[0].presente").value(true));
    }

    @ParameterizedTest(name = "el rol {0} recibe 403 en mi-asistencia")
    @ValueSource(strings = {"DOCENTE", "JEFE_CATEDRA"})
    @DisplayName("RN-15: solo el alumno consulta su asistencia")
    void deberiaResponder403_cuandoElRolNoEsAlumno(String rol) throws Exception {
        mockMvc.perform(get(RUTA_PROPIA).with(jwt().authorities(rol(rol))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("S06 sin token responde 401")
    void deberiaResponder401_alConsultarMiAsistenciaSinToken() throws Exception {
        mockMvc.perform(get(RUTA_PROPIA)).andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }
}
