package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.AsignacionDocenteResponse;
import ar.edu.utn.frc.atenas.catedra.service.AsignacionDocenteService;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsignacionDocenteController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class AsignacionDocenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionDocenteService asignacionDocenteService;

    @Test
    @DisplayName("C10 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoAsignaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/docentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"docenteId\":3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede asignar docentes")
    void deberiaResponderProhibido_cuandoDocenteAsigna() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"docenteId\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C10 con jefe responde 201 y Location")
    void deberiaAsignarDocente_cuandoEsJefe() throws Exception {
        when(asignacionDocenteService.asignar(eq(10L), any()))
                .thenReturn(new AsignacionDocenteResponse(20L, 3L, 10L));

        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"docenteId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/comisiones-lectivas/10/docentes/3")))
                .andExpect(jsonPath("$.docenteId").value(3));
    }

    @Test
    @DisplayName("C10 responde 400 cuando falta el docente")
    void deberiaResponderSolicitudInvalida_cuandoFaltaDocente() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C10 responde 404 cuando no existe la comisión")
    void deberiaResponderNoEncontrado_cuandoComisionNoExiste() throws Exception {
        when(asignacionDocenteService.asignar(eq(99L), any()))
                .thenThrow(new NoEncontradoException("No existe la comisión lectiva 99"));

        mockMvc.perform(post("/api/catedra/comisiones-lectivas/99/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"docenteId\":3}"))
                .andExpect(status().isNotFound());
    }
}
