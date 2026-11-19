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
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaRegistroService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaRegistroController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class,
        ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class EntregaRegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaRegistroService entregaRegistroService;

    @Test
    @DisplayName("D03 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoNoTieneToken() throws Exception {
        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D03 con jefe responde 403")
    void deberiaResponderProhibido_cuandoNoEsAlumno() throws Exception {
        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D03 con alumno responde 201")
    void deberiaRegistrarEntrega_cuandoEsAlumno() throws Exception {
        when(entregaRegistroService.registrar(org.mockito.ArgumentMatchers.eq(3L), any()))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    @DisplayName("D03 no representante responde 403")
    void deberiaResponderProhibido_cuandoFallaDominio() throws Exception {
        doThrow(new AccesoDenegadoException("no representante"))
                .when(entregaRegistroService).registrar(org.mockito.ArgumentMatchers.eq(3L), any());

        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D03 duplicada responde 409")
    void deberiaResponderConflicto_cuandoYaExiste() throws Exception {
        doThrow(new ConflictoException("duplicada"))
                .when(entregaRegistroService).registrar(org.mockito.ArgumentMatchers.eq(3L), any());

        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D03 archivo inválido responde 422")
    void deberiaResponderNoProcesable_cuandoArchivoInvalido() throws Exception {
        doThrow(new NoProcesableException("archivo inválido"))
                .when(entregaRegistroService).registrar(org.mockito.ArgumentMatchers.eq(3L), any());

        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("D03 responde 503 si una dependencia falla")
    void deberiaResponder503_cuandoFallaDependencia() throws Exception {
        doThrow(new ServicioNoDisponibleException("caído"))
                .when(entregaRegistroService).registrar(org.mockito.ArgumentMatchers.eq(3L), any());

        mockMvc.perform(post("/api/documentacion/hitos/3/entregas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isServiceUnavailable());
    }

    private String cuerpo() {
        return "{\"enlace\":\"https://drive.google.com/file/d/F1/view\"}";
    }

    private EntregaResponse respuesta() {
        return new EntregaResponse(99L, 3L, 7L, EstadoEntrega.ENTREGADA, false,
                OffsetDateTime.parse("2026-04-01T00:00:00-03:00"),
                "https://drive.google.com/file/d/F1/view", 50L);
    }
}
