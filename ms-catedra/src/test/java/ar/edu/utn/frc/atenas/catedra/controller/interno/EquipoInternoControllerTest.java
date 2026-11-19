package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.IntegranteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.EquipoService;
import ar.edu.utn.frc.atenas.commons.error.ManejadorGlobalDeErrores;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipoInternoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class EquipoInternoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipoService equipoService;

    @Test
    @DisplayName("I01 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoNoHayToken() throws Exception {
        mockMvc.perform(get("/internal/equipos/20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I01 acepta el rol SERVICIO")
    void deberiaResponderEquipo_cuandoEsServicio() throws Exception {
        when(equipoService.obtenerInterno(20L)).thenReturn(equipoInterno());

        mockMvc.perform(get("/internal/equipos/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.SERVICIO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.integrantes[0].legajo").value("411626"));
    }

    @Test
    @DisplayName("I01 responde 404 cuando el equipo no existe")
    void deberiaResponderNoEncontrado_cuandoEquipoNoExiste() throws Exception {
        when(equipoService.obtenerInterno(99L))
                .thenThrow(new NoEncontradoException("No existe el equipo 99"));

        mockMvc.perform(get("/internal/equipos/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("I02 lista equipos por comisión")
    void deberiaListarEquipos_cuandoSeFiltraPorComision() throws Exception {
        when(equipoService.listarInterno(10L, null)).thenReturn(List.of(equipoInterno()));

        mockMvc.perform(get("/internal/equipos").param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    @DisplayName("I02 responde 400 sin filtros o con ambos")
    void deberiaResponderSolicitudInvalida_cuandoLosFiltrosNoSonExactos() throws Exception {
        when(equipoService.listarInterno(null, null))
                .thenThrow(new SolicitudInvalidaException("Se debe indicar exactamente un filtro"));

        mockMvc.perform(get("/internal/equipos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isBadRequest());
    }

    private EquipoInternoResponse equipoInterno() {
        return new EquipoInternoResponse(
                20L,
                "Equipo 1",
                10L,
                2L,
                "ABIERTO",
                false,
                "sub-alumno",
                List.of(new IntegranteInternoResponse(1L, "sub-alumno", "411626", null)));
    }
}
