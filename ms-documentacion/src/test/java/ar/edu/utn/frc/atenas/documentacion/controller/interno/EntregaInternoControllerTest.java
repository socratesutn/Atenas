package ar.edu.utn.frc.atenas.documentacion.controller.interno;

import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaInternaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaConsultaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class,
        ZonaHoraria.class})
class EntregaInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaConsultaService entregaConsultaService;

    @Test
    @DisplayName("I07 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoI07NoTieneToken() throws Exception {
        mockMvc.perform(get("/internal/entregas")
                        .param("cursoLectivoId", "2")
                        .param("equipoIds", "1,2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I07 con jefe responde 200 y el contrato mínimo")
    void deberiaResponderOk_cuandoI07LoPideElJefe() throws Exception {
        when(entregaConsultaService.listarInterno(2L, List.of(1L, 2L)))
                .thenReturn(List.of(new EntregaInternaResponse(7L, 3L, "PENDIENTE", false, null)));

        mockMvc.perform(get("/internal/entregas")
                        .param("cursoLectivoId", "2")
                        .param("equipoIds", "1,2")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoId").value(7))
                .andExpect(jsonPath("$[0].hitoId").value(3))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("I07 con docente responde 200")
    void deberiaResponderOk_cuandoI07LoPideUnDocente() throws Exception {
        when(entregaConsultaService.listarInterno(2L, List.of(1L)))
                .thenReturn(List.of());

        mockMvc.perform(get("/internal/entregas")
                        .param("cursoLectivoId", "2")
                        .param("equipoIds", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("I07 con alumno responde 403")
    void deberiaResponderProhibido_cuandoI07LoPideUnAlumno() throws Exception {
        mockMvc.perform(get("/internal/entregas")
                        .param("cursoLectivoId", "2")
                        .param("equipoIds", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I07 con rol SERVICIO responde 403")
    void deberiaResponderProhibido_cuandoI07LoPideElServicio() throws Exception {
        mockMvc.perform(get("/internal/entregas")
                        .param("cursoLectivoId", "2")
                        .param("equipoIds", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.SERVICIO))))
                .andExpect(status().isForbidden());
    }
}
