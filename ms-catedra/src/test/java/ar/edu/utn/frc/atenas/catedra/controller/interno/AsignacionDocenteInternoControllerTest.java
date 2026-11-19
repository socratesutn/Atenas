package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteAsignadoResponse;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsignacionDocenteInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class AsignacionDocenteInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionDocenteService asignacionDocenteService;

    @Test
    @DisplayName("I04 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoNoHayToken() throws Exception {
        mockMvc.perform(get("/internal/comisiones-lectivas/10/docentes/sub-docente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I04 responde si el docente está asignado")
    void deberiaResponderAsignado_cuandoHayToken() throws Exception {
        when(asignacionDocenteService.estaAsignado(10L, "sub-docente"))
                .thenReturn(new DocenteAsignadoResponse(true));

        mockMvc.perform(get("/internal/comisiones-lectivas/10/docentes/sub-docente")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignado").value(true));
    }

    @Test
    @DisplayName("I04 responde 404 cuando la comisión no existe")
    void deberiaResponderNoEncontrado_cuandoComisionNoExiste() throws Exception {
        when(asignacionDocenteService.estaAsignado(99L, "sub-docente"))
                .thenThrow(new NoEncontradoException("No existe la comisión lectiva 99"));

        mockMvc.perform(get("/internal/comisiones-lectivas/99/docentes/sub-docente")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }
}
