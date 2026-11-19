package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.CursoLectivoService;
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

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CursoLectivoInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class CursoLectivoInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoLectivoService cursoLectivoService;

    @Test
    @DisplayName("I05 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoNoHayToken() throws Exception {
        mockMvc.perform(get("/internal/cursos-lectivos/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I05 responde el curso cuando hay token")
    void deberiaResponderCurso_cuandoHayToken() throws Exception {
        when(cursoLectivoService.obtenerInterno(2L))
                .thenReturn(new CursoLectivoInternoResponse(
                        2L, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));

        mockMvc.perform(get("/internal/cursos-lectivos/2")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.anio").value(2026));
    }

    @Test
    @DisplayName("I05 responde 404 cuando el curso no existe")
    void deberiaResponderNoEncontrado_cuandoCursoNoExiste() throws Exception {
        when(cursoLectivoService.obtenerInterno(99L))
                .thenThrow(new NoEncontradoException("No existe el curso lectivo 99"));

        mockMvc.perform(get("/internal/cursos-lectivos/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }
}
