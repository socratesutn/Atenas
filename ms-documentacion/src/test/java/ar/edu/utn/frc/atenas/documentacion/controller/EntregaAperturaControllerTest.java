package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaAperturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaAperturaController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class,
        ZonaHoraria.class})
class EntregaAperturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaAperturaService entregaAperturaService;

    @Test
    @DisplayName("D06 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoD06NoTieneToken() throws Exception {
        mockMvc.perform(get("/api/documentacion/entregas/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D06 con jefe responde 403")
    void deberiaResponderProhibido_cuandoD06LoPideUnJefe() throws Exception {
        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D06 con alumno responde 403")
    void deberiaResponderProhibido_cuandoD06LoPideUnAlumno() throws Exception {
        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D06 con docente responde 200 con hito y cambio")
    void deberiaAbrirLaEntrega_cuandoD06LoPideUnDocente() throws Exception {
        when(entregaAperturaService.abrir(10L)).thenReturn(apertura());

        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.hito.id").value(3))
                .andExpect(jsonPath("$.hito.nombre").value("Sprint 1"))
                .andExpect(jsonPath("$.modificadoPostEntrega").value("SI"));
    }

    @Test
    @DisplayName("D06 con docente no asignado responde 403")
    void deberiaResponderProhibido_cuandoFallaLaReglaDeDominio() throws Exception {
        doThrow(new AccesoDenegadoException("no asignado", "RN-01"))
                .when(entregaAperturaService).abrir(10L);

        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D06 con entrega inexistente responde 404")
    void deberiaResponderNoEncontrado_cuandoLaEntregaNoExiste() throws Exception {
        when(entregaAperturaService.abrir(10L))
                .thenThrow(new NoEncontradoException("no existe"));

        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D06 responde 503 cuando ms-catedra no responde")
    void deberiaResponderServicioNoDisponible_cuandoFallaCatedra() throws Exception {
        when(entregaAperturaService.abrir(10L))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        mockMvc.perform(get("/api/documentacion/entregas/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isServiceUnavailable());
    }

    private EntregaAperturaResponse apertura() {
        return new EntregaAperturaResponse(
                10L,
                new EntregaAperturaResponse.HitoApertura(
                        3L,
                        "Sprint 1",
                        "SPRINT",
                        OffsetDateTime.parse("2026-03-01T00:00:00-03:00"),
                        OffsetDateTime.parse("2026-03-15T23:59:59-03:00")),
                7L,
                "ENTREGADA",
                false,
                null,
                "https://drive.google.com/file/d/file-1",
                CambioPostEntrega.SI);
    }
}
