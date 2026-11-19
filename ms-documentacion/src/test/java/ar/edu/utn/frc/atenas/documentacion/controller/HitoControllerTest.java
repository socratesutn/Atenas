package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.HitoResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.service.HitoService;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HitoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class,
        ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class HitoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HitoService hitoService;

    @Test
    @DisplayName("D01 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoD01NoTieneToken() throws Exception {
        mockMvc.perform(post("/api/documentacion/hitos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D01 con jefe responde 201")
    void deberiaCrearHito_cuandoEsJefe() throws Exception {
        when(hitoService.crear(org.mockito.ArgumentMatchers.any())).thenReturn(hito());

        mockMvc.perform(post("/api/documentacion/hitos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("D01 con docente responde 403")
    void deberiaResponderProhibido_cuandoDocenteCreaHito() throws Exception {
        mockMvc.perform(post("/api/documentacion/hitos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D01 responde 503 si ms-catedra no responde")
    void deberiaResponder503_cuandoFallaCatedra() throws Exception {
        when(hitoService.crear(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        mockMvc.perform(post("/api/documentacion/hitos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("D02 responde 200 con la lista")
    void deberiaListarHitos() throws Exception {
        when(hitoService.listar(2L)).thenReturn(List.of(hito()));

        mockMvc.perform(get("/api/documentacion/hitos")
                        .param("cursoLectivoId", "2")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    private String cuerpo() {
        return """
                {"cursoLectivoId":2,"nombre":"Sprint 1","tipo":"SPRINT","orden":1,
                 "fechaInicio":"2026-04-01T00:00:00Z","fechaLimite":"2026-04-30T00:00:00Z"}
                """;
    }

    private HitoResponse hito() {
        return new HitoResponse(10L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                OffsetDateTime.parse("2026-04-01T00:00:00-03:00"),
                OffsetDateTime.parse("2026-04-30T00:00:00-03:00"));
    }
}
