package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.SecurityConfig;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.service.ActualizacionActividadService;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion.DetalleSprint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S08: códigos HTTP, resumen de snapshots y 401/403/404. */
@WebMvcTest(ActividadController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ZonaHoraria.class})
class ActividadControllerTest {

    private static final String RUTA = "/api/seguimiento/equipos/7/actividad/actualizacion";
    private static final Instant AHORA = Instant.parse("2026-11-20T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActualizacionActividadService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder pedidoConRol(String rol) {
        return post(RUTA).with(jwt().authorities(new SimpleGrantedAuthority(rol)));
    }

    private ResultadoActualizacion resultado() {
        RepositorioVinculado repositorio = RepositorioVinculado.builder().id(1L).equipoId(7L).build();
        SnapshotActividad snapshot = SnapshotActividad.builder().id(5L).repositorio(repositorio)
                .hitoId(3L).fechaConsulta(AHORA).estado(EstadoSnapshot.ACTUALIZADO)
                .commitsTotales(8).commitsSinAtribuir(2).build();
        return new ResultadoActualizacion(7L, AHORA,
                List.of(new DetalleSprint(3L, "Sprint 1", snapshot, false)));
    }

    @Test
    @DisplayName("S08: un docente actualiza la actividad y recibe 200 con el resumen del sprint")
    void deberiaResponder200_cuandoElDocenteActualizaLaActividad() throws Exception {
        when(servicio.actualizarEquipo(7L)).thenReturn(resultado());

        mockMvc.perform(pedidoConRol("ROLE_DOCENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipoId").value(7))
                .andExpect(jsonPath("$.fechaActualizacion").isNotEmpty())
                .andExpect(jsonPath("$.snapshots[0].hitoId").value(3))
                .andExpect(jsonPath("$.snapshots[0].nombreHito").value("Sprint 1"))
                .andExpect(jsonPath("$.snapshots[0].estado").value("ACTUALIZADO"))
                .andExpect(jsonPath("$.snapshots[0].commitsTotales").value(8))
                .andExpect(jsonPath("$.snapshots[0].commitsSinAtribuir").value(2))
                .andExpect(jsonPath("$.snapshots[0].omitido").value(false));
    }

    @Test
    @DisplayName("S08: un jefe también puede actualizar la actividad")
    void deberiaResponder200_cuandoElJefeActualizaLaActividad() throws Exception {
        when(servicio.actualizarEquipo(7L)).thenReturn(resultado());

        mockMvc.perform(pedidoConRol("ROLE_JEFE_CATEDRA"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin token responde 401 y no llega al servicio")
    void deberiaResponder401_cuandoNoHayToken() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Un alumno responde 403 y no llega al servicio")
    void deberiaResponder403_cuandoElRolEsAlumno() throws Exception {
        mockMvc.perform(pedidoConRol("ROLE_ALUMNO"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "el rol {0} recibe 403")
    @ValueSource(strings = {"ROLE_SERVICIO", "ROLE_OTRO"})
    @DisplayName("Otros roles no pueden invocar S08")
    void deberiaResponder403_cuandoElRolNoEstaAutorizado(String rol) throws Exception {
        mockMvc.perform(pedidoConRol(rol))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("RN-31: un docente de otra comisión responde 403 con la regla en el ProblemDetail")
    void deberiaResponder403_cuandoElDocenteNoEsDeLaComision() throws Exception {
        when(servicio.actualizarEquipo(anyLong()))
                .thenThrow(new AccesoDenegadoException("El docente no es de la comisión", "RN-01"));

        mockMvc.perform(pedidoConRol("ROLE_DOCENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.regla").value("RN-01"));
    }

    @Test
    @DisplayName("S08: responde 404 cuando el equipo no tiene repositorio vinculado")
    void deberiaResponder404_cuandoNoHayRepositorioVinculado() throws Exception {
        when(servicio.actualizarEquipo(7L))
                .thenThrow(new NoEncontradoException("El equipo no tiene repositorio vinculado"));

        mockMvc.perform(pedidoConRol("ROLE_DOCENTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
