package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.ComisionResponse;
import ar.edu.utn.frc.atenas.catedra.service.ComisionService;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
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

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComisionController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class ComisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComisionService comisionService;

    @Test
    @DisplayName("C03 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Comisión 1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede crear comisiones")
    void deberiaResponderProhibido_cuandoDocenteCreaComision() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Comisión 1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C03 con jefe responde 201 y Location")
    void deberiaCrearComision_cuandoEsJefe() throws Exception {
        when(comisionService.crear(any())).thenReturn(new ComisionResponse(1L, "Comisión 1"));

        mockMvc.perform(post("/api/catedra/comisiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Comisión 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/comisiones/1")))
                .andExpect(jsonPath("$.nombre").value("Comisión 1"));
    }

    @Test
    @DisplayName("C03 responde 400 cuando el nombre está vacío")
    void deberiaResponderSolicitudInvalida_cuandoNombreVacio() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C04 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/comisiones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede listar comisiones")
    void deberiaResponderProhibido_cuandoDocenteListaComisiones() throws Exception {
        mockMvc.perform(get("/api/catedra/comisiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C04 con jefe responde 200")
    void deberiaListarComisiones_cuandoEsJefe() throws Exception {
        when(comisionService.listar()).thenReturn(List.of(new ComisionResponse(1L, "Comisión 1")));

        mockMvc.perform(get("/api/catedra/comisiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Comisión 1"));
    }
}
