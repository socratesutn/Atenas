package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
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

@WebMvcTest(EstudianteController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class EstudianteControllerTest {

    private static final String CUERPO = """
            {"legajo":"411626","nombre":"Alumno","apellido":"Prueba","comisionLectivaId":10}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @Test
    @DisplayName("C11 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede crear estudiantes")
    void deberiaResponderProhibido_cuandoDocenteCreaEstudiante() throws Exception {
        mockMvc.perform(post("/api/catedra/estudiantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C11 con jefe responde 201 y Location")
    void deberiaCrearEstudiante_cuandoEsJefe() throws Exception {
        when(estudianteService.crear(any())).thenReturn(estudianteResponse(true));

        mockMvc.perform(post("/api/catedra/estudiantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/estudiantes/1")))
                .andExpect(jsonPath("$.mail").value("411626@sistemas.frc.utn.edu.ar"));
    }

    @Test
    @DisplayName("RN-02: C11 responde 400 con legajo no numérico")
    void deberiaResponderSolicitudInvalida_cuandoLegajoNoEsNumerico() throws Exception {
        mockMvc.perform(post("/api/catedra/estudiantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legajo\":\"41A626\",\"nombre\":\"Alumno\",\"apellido\":\"Prueba\",\"comisionLectivaId\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C12 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/comisiones-lectivas/10/estudiantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un alumno no puede listar estudiantes")
    void deberiaResponderProhibido_cuandoAlumnoListaEstudiantes() throws Exception {
        mockMvc.perform(get("/api/catedra/comisiones-lectivas/10/estudiantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C12 con docente responde 200")
    void deberiaListarEstudiantes_cuandoEsDocente() throws Exception {
        when(estudianteService.listarPorComision(10L)).thenReturn(List.of(estudianteResponse(true)));

        mockMvc.perform(get("/api/catedra/comisiones-lectivas/10/estudiantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].legajo").value("411626"));
    }

    @Test
    @DisplayName("C13 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoDaDeBajaSinToken() throws Exception {
        mockMvc.perform(patch("/api/catedra/estudiantes/1/baja"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede dar de baja estudiantes")
    void deberiaResponderProhibido_cuandoDocenteDaDeBaja() throws Exception {
        mockMvc.perform(patch("/api/catedra/estudiantes/1/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C13 con jefe responde 200")
    void deberiaDarDeBaja_cuandoEsJefe() throws Exception {
        when(estudianteService.darDeBaja(1L)).thenReturn(estudianteResponse(false));

        mockMvc.perform(patch("/api/catedra/estudiantes/1/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("RN-05: C13 responde 409 cuando el equipo está asegurado")
    void deberiaResponderConflicto_cuandoEquipoEstaAsegurado() throws Exception {
        when(estudianteService.darDeBaja(1L))
                .thenThrow(new ConflictoException("El equipo está asegurado", "RN-05"));

        mockMvc.perform(patch("/api/catedra/estudiantes/1/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C13 responde 404 cuando el estudiante no existe")
    void deberiaResponderNoEncontrado_cuandoEstudianteNoExiste() throws Exception {
        when(estudianteService.darDeBaja(99L))
                .thenThrow(new NoEncontradoException("No existe el estudiante 99"));

        mockMvc.perform(patch("/api/catedra/estudiantes/99/baja")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }

    private EstudianteResponse estudianteResponse(boolean activo) {
        return new EstudianteResponse(
                1L,
                "411626",
                "Alumno",
                "Prueba",
                "411626@sistemas.frc.utn.edu.ar",
                null,
                activo,
                10L);
    }
}
