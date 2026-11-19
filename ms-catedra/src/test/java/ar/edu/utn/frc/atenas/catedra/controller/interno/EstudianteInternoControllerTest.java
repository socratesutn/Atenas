package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstudianteInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class EstudianteInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @Test
    @DisplayName("I03 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoNoHayToken() throws Exception {
        mockMvc.perform(get("/internal/estudiantes/por-sub/sub-alumno"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I03 responde la pertenencia del alumno")
    void deberiaResponderPertenencia_cuandoHayToken() throws Exception {
        when(estudianteService.obtenerInternoPorSub("sub-alumno"))
                .thenReturn(new EstudianteInternoResponse(
                        1L, "sub-alumno", true, 10L, 2L, null));

        mockMvc.perform(get("/internal/estudiantes/por-sub/sub-alumno")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value(1))
                .andExpect(jsonPath("$.comisionLectivaId").value(10))
                .andExpect(jsonPath("$.equipoId").value(nullValue()));
    }

    @Test
    @DisplayName("I03 responde 404 cuando no existe el estudiante")
    void deberiaResponderNoEncontrado_cuandoNoExisteEstudiante() throws Exception {
        when(estudianteService.obtenerInternoPorSub("sub-desconocido"))
                .thenThrow(new NoEncontradoException("No existe un estudiante con ese sub"));

        mockMvc.perform(get("/internal/estudiantes/por-sub/sub-desconocido")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }
}
