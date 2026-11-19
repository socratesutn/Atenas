package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaCorreccionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaCorreccionController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class,
        ZonaHoraria.class})
class EntregaCorreccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaCorreccionService entregaCorreccionService;

    @Test
    @DisplayName("D07 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoD07NoTieneToken() throws Exception {
        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D07 con jefe responde 403")
    void deberiaResponderProhibido_cuandoD07LoPideUnJefe() throws Exception {
        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D07 con docente responde 200")
    void deberiaCorregir_cuandoD07LoPideUnDocente() throws Exception {
        when(entregaCorreccionService.corregir(10L, 8)).thenReturn(respuesta());

        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CORREGIDA"))
                .andExpect(jsonPath("$.nota").value(8));
    }

    @Test
    @DisplayName("D07 con docente no asignado responde 403")
    void deberiaResponderProhibido_cuandoFallaLaReglaDeDominio() throws Exception {
        doThrow(new AccesoDenegadoException("no asignado", "RN-01"))
                .when(entregaCorreccionService).corregir(10L, 8);

        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D07 ya corregida responde 409")
    void deberiaResponderConflicto_cuandoYaEstaCorregida() throws Exception {
        doThrow(new ConflictoException("ya corregida", "RN-24"))
                .when(entregaCorreccionService).corregir(10L, 8);

        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D07 con nota fuera de rango responde 422")
    void deberiaResponderNoProcesable_cuandoLaNotaEsInvalida() throws Exception {
        doThrow(new NoProcesableException("nota inválida", "RN-24"))
                .when(entregaCorreccionService).corregir(10L, 0);

        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("D07 responde 503 cuando ms-catedra no responde")
    void deberiaResponderServicioNoDisponible_cuandoFallaCatedra() throws Exception {
        doThrow(new ServicioNoDisponibleException("caído"))
                .when(entregaCorreccionService).corregir(10L, 8);

        mockMvc.perform(patch("/api/documentacion/entregas/10/correccion")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":8}"))
                .andExpect(status().isServiceUnavailable());
    }

    private EntregaAperturaResponse respuesta() {
        return new EntregaAperturaResponse(
                10L,
                new EntregaAperturaResponse.HitoApertura(
                        3L,
                        "Sprint 1",
                        "SPRINT",
                        OffsetDateTime.parse("2026-03-01T00:00:00-03:00"),
                        OffsetDateTime.parse("2026-03-15T23:59:59-03:00")),
                7L,
                "CORREGIDA",
                false,
                8,
                "https://drive.google.com/file/d/file-1",
                CambioPostEntrega.SI);
    }
}
