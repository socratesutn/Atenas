package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaConsultaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class,
        ZonaHoraria.class})
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaConsultaService entregaConsultaService;

    @Test
    @DisplayName("D04 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoD04NoTieneToken() throws Exception {
        mockMvc.perform(get("/api/documentacion/entregas")
                        .param("comisionLectivaId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D04 con alumno responde 403")
    void deberiaResponderProhibido_cuandoD04LoPideUnAlumno() throws Exception {
        mockMvc.perform(get("/api/documentacion/entregas")
                        .param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D04 con jefe responde 200 y el estado calculado")
    void deberiaListarEntregas_cuandoD04LoPideElJefe() throws Exception {
        when(entregaConsultaService.listarPorComision(10L, null))
                .thenReturn(List.of(entrega()));

        mockMvc.perform(get("/api/documentacion/entregas")
                        .param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoId").value(7))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].hito.nombre").value("Sprint 1"))
                .andExpect(jsonPath("$[0].entregaId").value(nullValue()));
    }

    @Test
    @DisplayName("D04 con docente no asignado responde 403")
    void deberiaResponderProhibido_cuandoD04FallaLaReglaDeDominio() throws Exception {
        doThrow(new AccesoDenegadoException("no asignado", "RN-01"))
                .when(entregaConsultaService).listarPorComision(10L, null);

        mockMvc.perform(get("/api/documentacion/entregas")
                        .param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D04 responde 503 cuando ms-catedra no responde")
    void deberiaResponderServicioNoDisponible_cuandoD04FallaCatedra() throws Exception {
        when(entregaConsultaService.listarPorComision(10L, null))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        mockMvc.perform(get("/api/documentacion/entregas")
                        .param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("D05 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoD05NoTieneToken() throws Exception {
        mockMvc.perform(get("/api/documentacion/mi-equipo/entregas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D05 con jefe responde 403")
    void deberiaResponderProhibido_cuandoD05LoPideUnJefe() throws Exception {
        mockMvc.perform(get("/api/documentacion/mi-equipo/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D05 con alumno responde 200")
    void deberiaListarEntregas_cuandoD05LoPideUnAlumno() throws Exception {
        when(entregaConsultaService.listarMiEquipo()).thenReturn(List.of(entrega()));

        mockMvc.perform(get("/api/documentacion/mi-equipo/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    private EntregaEstadoResponse entrega() {
        return new EntregaEstadoResponse(
                7L,
                3L,
                new EntregaEstadoResponse.HitoResumen(
                        "Sprint 1",
                        "SPRINT",
                        OffsetDateTime.parse("2026-03-01T00:00:00-03:00"),
                        OffsetDateTime.parse("2026-03-15T23:59:59-03:00")),
                "PENDIENTE",
                false,
                null,
                null);
    }
}
