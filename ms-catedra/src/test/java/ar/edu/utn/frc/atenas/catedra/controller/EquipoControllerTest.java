package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.config.SecurityConfig;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResumenResponse;
import ar.edu.utn.frc.atenas.catedra.dto.IntegranteResponse;
import ar.edu.utn.frc.atenas.catedra.service.EquipoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipoController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ManejadorGlobalDeErrores.class, ZonaHoraria.class})
class EquipoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipoService equipoService;

    @Test
    @DisplayName("C16 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoCreaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/equipos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Equipo 1\",\"cupoMinimo\":2,\"cupoMaximo\":3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un jefe no crea equipos por esta ruta")
    void deberiaResponderProhibido_cuandoJefeCreaEquipo() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/equipos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Equipo 1\",\"cupoMinimo\":2,\"cupoMaximo\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C16 con docente responde 201 y Location")
    void deberiaCrearEquipo_cuandoEsDocente() throws Exception {
        when(equipoService.crear(eq(10L), any())).thenReturn(equipoResponse());

        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/equipos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Equipo 1\",\"cupoMinimo\":2,\"cupoMaximo\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/catedra/equipos/20")))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
    }

    @Test
    @DisplayName("RN-06: C16 responde 400 con cupo inválido")
    void deberiaResponderSolicitudInvalida_cuandoCupoEsInvalido() throws Exception {
        mockMvc.perform(post("/api/catedra/comisiones-lectivas/10/equipos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Equipo 1\",\"cupoMinimo\":0,\"cupoMaximo\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C17 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoListaSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/comisiones-lectivas/10/equipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C17 con alumno responde 200")
    void deberiaListarEquipos_cuandoEsAlumno() throws Exception {
        when(equipoService.listarPorComision(10L)).thenReturn(List.of(equipoResumen()));

        mockMvc.perform(get("/api/catedra/comisiones-lectivas/10/equipos")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(true));
    }

    @Test
    @DisplayName("C18 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoConsultaEquipoSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/equipos/20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C18 con docente responde 200")
    void deberiaConsultarEquipo_cuandoEsDocente() throws Exception {
        when(equipoService.obtener(20L)).thenReturn(equipoResponse());

        mockMvc.perform(get("/api/catedra/equipos/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    @DisplayName("C18 responde 404 cuando el equipo no existe")
    void deberiaResponderNoEncontrado_cuandoEquipoNoExiste() throws Exception {
        when(equipoService.obtener(99L)).thenThrow(new NoEncontradoException("No existe el equipo 99"));

        mockMvc.perform(get("/api/catedra/equipos/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C19 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoIngresaSinToken() throws Exception {
        mockMvc.perform(post("/api/catedra/equipos/20/integrantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-01: un docente no puede ingresar como alumno")
    void deberiaResponderProhibido_cuandoDocenteIngresa() throws Exception {
        mockMvc.perform(post("/api/catedra/equipos/20/integrantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C19 con alumno responde 201")
    void deberiaIngresarAlEquipo_cuandoEsAlumno() throws Exception {
        when(equipoService.ingresar(20L)).thenReturn(equipoResponse());

        mockMvc.perform(post("/api/catedra/equipos/20/integrantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("C19 responde 409 cuando el equipo está asegurado")
    void deberiaResponderConflicto_cuandoNoSePuedeIngresar() throws Exception {
        when(equipoService.ingresar(20L))
                .thenThrow(new ConflictoException("Equipo asegurado", "RN-08"));

        mockMvc.perform(post("/api/catedra/equipos/20/integrantes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C20 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoSaleSinToken() throws Exception {
        mockMvc.perform(delete("/api/catedra/equipos/20/integrantes/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-09: un docente no puede usar la salida de alumno")
    void deberiaResponderProhibido_cuandoDocenteSale() throws Exception {
        mockMvc.perform(delete("/api/catedra/equipos/20/integrantes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C20 con alumno responde 204")
    void deberiaSalir_cuandoEsAlumno() throws Exception {
        mockMvc.perform(delete("/api/catedra/equipos/20/integrantes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("C20 responde 409 cuando el equipo está asegurado")
    void deberiaResponderConflicto_cuandoSaleDeEquipoAsegurado() throws Exception {
        doThrow(new ConflictoException("Equipo asegurado", "RN-09"))
                .when(equipoService).salir(20L, 1L);

        mockMvc.perform(delete("/api/catedra/equipos/20/integrantes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C21 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoAseguraSinToken() throws Exception {
        mockMvc.perform(patch("/api/catedra/equipos/20/aseguramiento"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-10: un jefe no asegura equipos por esta ruta")
    void deberiaResponderProhibido_cuandoJefeAsegura() throws Exception {
        mockMvc.perform(patch("/api/catedra/equipos/20/aseguramiento")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.JEFE_CATEDRA))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C21 con alumno responde 200")
    void deberiaAsegurar_cuandoEsAlumno() throws Exception {
        when(equipoService.asegurar(20L)).thenReturn(equipoResponse());

        mockMvc.perform(patch("/api/catedra/equipos/20/aseguramiento")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C21 responde 409 cuando no cumple las condiciones")
    void deberiaResponderConflicto_cuandoNoPuedeAsegurar() throws Exception {
        when(equipoService.asegurar(20L))
                .thenThrow(new ConflictoException("Falta GitHub", "RN-10"));

        mockMvc.perform(patch("/api/catedra/equipos/20/aseguramiento")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C22 sin token responde 401")
    void deberiaResponderNoAutorizado_cuandoReabreSinToken() throws Exception {
        mockMvc.perform(patch("/api/catedra/equipos/20/reapertura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-11: un alumno no puede reabrir")
    void deberiaResponderProhibido_cuandoAlumnoReabre() throws Exception {
        mockMvc.perform(patch("/api/catedra/equipos/20/reapertura")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.ALUMNO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C22 con docente responde 200")
    void deberiaReabrir_cuandoEsDocente() throws Exception {
        when(equipoService.reabrir(20L)).thenReturn(equipoResponse());

        mockMvc.perform(patch("/api/catedra/equipos/20/reapertura")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C22 responde 409 cuando el equipo ya está abierto")
    void deberiaResponderConflicto_cuandoYaEstaAbierto() throws Exception {
        when(equipoService.reabrir(20L))
                .thenThrow(new ConflictoException("Equipo abierto", "RN-11"));

        mockMvc.perform(patch("/api/catedra/equipos/20/reapertura")
                        .with(jwt().authorities(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.DOCENTE))))
                .andExpect(status().isConflict());
    }

    private EquipoResponse equipoResponse() {
        return new EquipoResponse(
                20L,
                "Equipo 1",
                10L,
                2L,
                "ABIERTO",
                false,
                2,
                3,
                1,
                true,
                "sub-alumno",
                List.of(new IntegranteResponse(1L, "sub-alumno", "411626", null)));
    }

    private EquipoResumenResponse equipoResumen() {
        return new EquipoResumenResponse(
                20L, "Equipo 1", 10L, 2L, "ABIERTO", false, 2, 3, 1, true, "sub-alumno");
    }
}
