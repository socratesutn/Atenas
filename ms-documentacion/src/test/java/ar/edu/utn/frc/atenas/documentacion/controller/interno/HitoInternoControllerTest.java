package ar.edu.utn.frc.atenas.documentacion.controller.interno;

import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.config.SecurityConfig;
import ar.edu.utn.frc.atenas.documentacion.dto.HitoResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.service.HitoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HitoInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class,
        ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class HitoInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HitoService hitoService;

    @Test
    @DisplayName("I06 con SERVICIO responde 200")
    void deberiaResponderOk_cuandoLoPideElServicio() throws Exception {
        when(hitoService.internos(2L, "SPRINT")).thenReturn(List.of(hito()));

        mockMvc.perform(get("/internal/hitos")
                        .param("cursoLectivoId", "2")
                        .param("tipo", "SPRINT")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.SERVICIO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("I06 con ALUMNO responde 403")
    void deberiaResponderProhibido_cuandoLoPideUnAlumno() throws Exception {
        mockMvc.perform(get("/internal/hitos")
                        .param("cursoLectivoId", "2")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    private HitoResponse hito() {
        return new HitoResponse(10L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                OffsetDateTime.parse("2026-04-01T00:00:00-03:00"),
                OffsetDateTime.parse("2026-04-30T00:00:00-03:00"));
    }
}
