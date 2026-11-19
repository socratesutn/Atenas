package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.MiPerfilResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MiPerfilController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class MiPerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @Test
    @DisplayName("C14 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoConsultaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/mi-perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-15: un jefe no puede consultar el perfil de alumno")
    void deberiaResponderProhibido_cuandoJefeConsultaMiPerfil() throws Exception {
        mockMvc.perform(get("/api/catedra/mi-perfil")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C14 con alumno responde 200 y equipo nulo")
    void deberiaResponderMiPerfil_cuandoEsAlumno() throws Exception {
        when(estudianteService.miPerfil()).thenReturn(perfil("alumno-uno"));

        mockMvc.perform(get("/api/catedra/mi-perfil")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value("411626"))
                .andExpect(jsonPath("$.equipoId").value(nullValue()));
    }

    @Test
    @DisplayName("C15 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoActualizaSinToken() throws Exception {
        mockMvc.perform(put("/api/catedra/mi-perfil/usuario-github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioGithub\":\"alumno-uno\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-26: C15 con alumno responde 200")
    void deberiaActualizarGithub_cuandoEsAlumno() throws Exception {
        when(estudianteService.actualizarUsuarioGithub(any())).thenReturn(perfil("alumno-uno"));

        mockMvc.perform(put("/api/catedra/mi-perfil/usuario-github")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioGithub\":\"Alumno-Uno\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioGithub").value("alumno-uno"));
    }

    @Test
    @DisplayName("RN-26: C15 responde 400 con formato inválido")
    void deberiaResponderSolicitudInvalida_cuandoGithubEsInvalido() throws Exception {
        mockMvc.perform(put("/api/catedra/mi-perfil/usuario-github")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioGithub\":\"alumno_con_guion_bajo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RN-26: C15 responde 409 con usuario duplicado")
    void deberiaResponderConflicto_cuandoGithubEstaDuplicado() throws Exception {
        when(estudianteService.actualizarUsuarioGithub(any()))
                .thenThrow(new ConflictoException("Usuario duplicado"));

        mockMvc.perform(put("/api/catedra/mi-perfil/usuario-github")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioGithub\":\"alumno-uno\"}"))
                .andExpect(status().isConflict());
    }

    private MiPerfilResponse perfil(String usuarioGithub) {
        return new MiPerfilResponse(
                1L,
                "411626",
                "Alumno",
                "Prueba",
                "411626@sistemas.frc.utn.edu.ar",
                usuarioGithub,
                true,
                10L,
                "Comisión 1",
                2L,
                2026,
                null);
    }
}
