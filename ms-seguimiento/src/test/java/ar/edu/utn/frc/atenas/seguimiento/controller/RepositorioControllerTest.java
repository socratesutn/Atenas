package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.SecurityConfig;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.service.RepositorioVinculadoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S07: código HTTP, forma del ProblemDetail, 401 y 403 por rol, y validación 400. */
@WebMvcTest(RepositorioController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ZonaHoraria.class})
class RepositorioControllerTest {

    private static final String RUTA = "/api/seguimiento/equipos/7/repositorio";
    private static final String CUERPO_VALIDO = "{\"url\":\"https://github.com/usuario/repo\",\"rama\":\"main\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositorioVinculadoService servicio;

    // Spring Security necesita un decodificador de JWT; en estos tests el token lo aporta jwt().
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder pedidoDeAlumno(String cuerpo) {
        return put(RUTA)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private static RepositorioVinculado vinculado() {
        return RepositorioVinculado.builder().id(1L).equipoId(7L).url("https://github.com/Usuario/Repo")
                .propietario("Usuario").nombre("Repo").ramaSeguimiento("main")
                .fechaValidacion(Instant.parse("2026-11-18T12:00:00Z")).build();
    }

    @Test
    @DisplayName("RN-25: un alumno vincula el repositorio y recibe 200 con los datos validados")
    void deberiaResponder200_cuandoElAlumnoVinculaElRepositorio() throws Exception {
        when(servicio.vincular(7L, "https://github.com/usuario/repo", "main")).thenReturn(vinculado());

        mockMvc.perform(pedidoDeAlumno(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipoId").value(7))
                .andExpect(jsonPath("$.url").value("https://github.com/Usuario/Repo"))
                .andExpect(jsonPath("$.propietario").value("Usuario"))
                .andExpect(jsonPath("$.nombre").value("Repo"))
                .andExpect(jsonPath("$.rama").value("main"))
                .andExpect(jsonPath("$.fechaValidacion").isNotEmpty());
    }

    @Test
    @DisplayName("Sin token responde 401 y no llega al servicio")
    void deberiaResponder401_cuandoNoHayToken() throws Exception {
        mockMvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "el rol {0} recibe 403")
    @ValueSource(strings = {"ROLE_DOCENTE", "ROLE_JEFE_CATEDRA"})
    @DisplayName("Un rol distinto de ALUMNO responde 403 y no llega al servicio")
    void deberiaResponder403_cuandoElRolNoEsAlumno(String rol) throws Exception {
        mockMvc.perform(put(RUTA)
                        .with(jwt().authorities(new SimpleGrantedAuthority(rol)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Una URL vacía responde 400 con la lista de errores y no llega al servicio")
    void deberiaResponder400_cuandoLaUrlEstaVacia() throws Exception {
        mockMvc.perform(pedidoDeAlumno("{\"url\":\"\",\"rama\":\"main\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(servicio, never()).vincular(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Una rama ausente responde 400 y no llega al servicio")
    void deberiaResponder400_cuandoFaltaLaRama() throws Exception {
        mockMvc.perform(pedidoDeAlumno("{\"url\":\"https://github.com/usuario/repo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(servicio, never()).vincular(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("RN-25: si el alumno no es el representante responde 403 con la regla en el ProblemDetail")
    void deberiaResponder403ConLaRegla_cuandoNoEsElRepresentante() throws Exception {
        when(servicio.vincular(eq(7L), anyString(), anyString()))
                .thenThrow(new AccesoDenegadoException("Solo el representante puede vincular", "RN-25"));

        mockMvc.perform(pedidoDeAlumno(CUERPO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.regla").value("RN-25"));
    }

    @Test
    @DisplayName("Un equipo inexistente responde 404")
    void deberiaResponder404_cuandoElEquipoNoExiste() throws Exception {
        when(servicio.vincular(eq(7L), anyString(), anyString()))
                .thenThrow(new NoEncontradoException("No existe el equipo"));

        mockMvc.perform(pedidoDeAlumno(CUERPO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("RN-25: un repositorio privado o una rama inexistente responden 422")
    void deberiaResponder422_cuandoElRepositorioOLaRamaNoSonValidos() throws Exception {
        when(servicio.vincular(eq(7L), anyString(), anyString()))
                .thenThrow(new NoProcesableException("El repositorio no es público", "RN-25"));

        mockMvc.perform(pedidoDeAlumno(CUERPO_VALIDO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.regla").value("RN-25"));
    }

    @Test
    @DisplayName("RN-28: si ms-catedra o GitHub no responden, 503")
    void deberiaResponder503_cuandoUnaDependenciaNoResponde() throws Exception {
        when(servicio.vincular(eq(7L), anyString(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("GitHub no respondió"));

        mockMvc.perform(pedidoDeAlumno(CUERPO_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
