package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteResponse;
import ar.edu.utn.frc.atenas.catedra.service.DocenteService;
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

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocenteController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class DocenteControllerTest {

    private static final String CUERPO = """
            {"nombre":"Docente","apellido":"Prueba","mail":"docente@atenas.local"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocenteService docenteService;

    @Test
    @DisplayName("C07 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/docentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede crear docentes")
    void deberiaResponderProhibido_cuandoDocenteCreaDocente() throws Exception {
        mockMvc.perform(post("/api/catedra/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C07 con jefe responde 201 y Location")
    void deberiaCrearDocente_cuandoEsJefe() throws Exception {
        when(docenteService.crear(any()))
                .thenReturn(new DocenteResponse(1L, "Docente", "Prueba", "docente@atenas.local", true));

        mockMvc.perform(post("/api/catedra/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/docentes/1")))
                .andExpect(jsonPath("$.mail").value("docente@atenas.local"));
    }

    @Test
    @DisplayName("C07 responde 400 con un mail inválido")
    void deberiaResponderSolicitudInvalida_cuandoMailNoEsValido() throws Exception {
        mockMvc.perform(post("/api/catedra/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Docente\",\"apellido\":\"Prueba\",\"mail\":\"no-es-mail\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C08 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/docentes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un alumno no puede listar docentes")
    void deberiaResponderProhibido_cuandoAlumnoListaDocentes() throws Exception {
        mockMvc.perform(get("/api/catedra/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C08 con jefe responde 200")
    void deberiaListarDocentes_cuandoEsJefe() throws Exception {
        when(docenteService.listar()).thenReturn(List.of(
                new DocenteResponse(1L, "Docente", "Prueba", "docente@atenas.local", true)));

        mockMvc.perform(get("/api/catedra/docentes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mail").value("docente@atenas.local"));
    }

    @Test
    @DisplayName("C09 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoDaDeBajaSinToken() throws Exception {
        mockMvc.perform(patch("/api/catedra/docentes/1/baja"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede darse de baja a sí mismo por esta ruta")
    void deberiaResponderProhibido_cuandoDocenteIntentaDarDeBaja() throws Exception {
        mockMvc.perform(patch("/api/catedra/docentes/1/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C09 con jefe responde 200")
    void deberiaDarDeBaja_cuandoEsJefe() throws Exception {
        when(docenteService.darDeBaja(1L))
                .thenReturn(new DocenteResponse(1L, "Docente", "Prueba", "docente@atenas.local", false));

        mockMvc.perform(patch("/api/catedra/docentes/1/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("C09 responde 404 cuando el docente no existe")
    void deberiaResponderNoEncontrado_cuandoDocenteNoExiste() throws Exception {
        when(docenteService.darDeBaja(99L))
                .thenThrow(new NoEncontradoException("No existe el docente 99"));

        mockMvc.perform(patch("/api/catedra/docentes/99/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }
}
