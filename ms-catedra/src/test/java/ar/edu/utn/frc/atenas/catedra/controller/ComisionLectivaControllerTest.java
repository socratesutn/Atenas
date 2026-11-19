package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.ComisionLectivaResponse;
import ar.edu.utn.frc.atenas.catedra.service.ComisionLectivaService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComisionLectivaController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class ComisionLectivaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComisionLectivaService comisionLectivaService;

    @Test
    @DisplayName("C05 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comisionId\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede habilitar comisiones")
    void deberiaResponderProhibido_cuandoDocenteCreaComisionLectiva() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comisionId\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C05 con jefe responde 201 y Location")
    void deberiaCrearComisionLectiva_cuandoEsJefe() throws Exception {
        when(comisionLectivaService.crear(eq(2L), any()))
                .thenReturn(new ComisionLectivaResponse(10L, 5L, "Comisión 1", 2L));

        mockMvc.perform(post("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comisionId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/cursos-lectivos/2/comisiones-lectivas/10")))
                .andExpect(jsonPath("$.comisionId").value(5));
    }

    @Test
    @DisplayName("C05 responde 400 cuando falta la comisión")
    void deberiaResponderSolicitudInvalida_cuandoFaltaComision() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C06 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/cursos-lectivos/2/comisiones-lectivas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un alumno no puede listar comisiones lectivas")
    void deberiaResponderProhibido_cuandoAlumnoListaComisionesLectivas() throws Exception {
        mockMvc.perform(get("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C06 con docente responde 200")
    void deberiaListarComisionesLectivas_cuandoEsDocente() throws Exception {
        when(comisionLectivaService.listarPorCurso(2L))
                .thenReturn(List.of(new ComisionLectivaResponse(10L, 5L, "Comisión 1", 2L)));

        mockMvc.perform(get("/api/catedra/cursos-lectivos/2/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comisionNombre").value("Comisión 1"));
    }

    @Test
    @DisplayName("C06 responde 404 cuando el curso no existe")
    void deberiaResponderNoEncontrado_cuandoCursoNoExiste() throws Exception {
        when(comisionLectivaService.listarPorCurso(99L))
                .thenThrow(new NoEncontradoException("No existe el curso lectivo 99"));

        mockMvc.perform(get("/api/catedra/cursos-lectivos/99/comisiones-lectivas")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }
}
