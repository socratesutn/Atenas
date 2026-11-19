package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoResponse;
import ar.edu.utn.frc.atenas.catedra.service.CursoLectivoService;
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

import java.time.LocalDate;
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

@WebMvcTest(CursoLectivoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class CursoLectivoControllerTest {

    private static final String CUERPO_VALIDO = """
            {"anio":2026,"fechaInicio":"2026-03-01","fechaFin":"2026-12-15"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoLectivoService cursoLectivoService;

    @Test
    @DisplayName("C01 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede crear cursos lectivos")
    void deberiaResponderProhibido_cuandoDocenteCreaCurso() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C01 con jefe responde 201 y Location")
    void deberiaCrearCurso_cuandoEsJefe() throws Exception {
        when(cursoLectivoService.crear(any()))
                .thenReturn(new CursoLectivoResponse(1L, 2026,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));

        mockMvc.perform(post("/api/catedra/cursos-lectivos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/cursos-lectivos/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.anio").value(2026));
    }

    @Test
    @DisplayName("C01 responde 400 cuando falta el año")
    void deberiaResponderSolicitudInvalida_cuandoFaltaAnio() throws Exception {
        mockMvc.perform(post("/api/catedra/cursos-lectivos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-03-01\",\"fechaFin\":\"2026-12-15\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C02 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/cursos-lectivos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un alumno no puede listar cursos lectivos")
    void deberiaResponderProhibido_cuandoAlumnoListaCursos() throws Exception {
        mockMvc.perform(get("/api/catedra/cursos-lectivos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C02 con docente responde 200")
    void deberiaListarCursos_cuandoEsDocente() throws Exception {
        when(cursoLectivoService.listar()).thenReturn(List.of(
                new CursoLectivoResponse(1L, 2026,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15))));

        mockMvc.perform(get("/api/catedra/cursos-lectivos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anio").value(2026));
    }
}
