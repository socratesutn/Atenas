package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.SecurityConfig;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.service.DashboardService;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ActividadEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.AsistenciaEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ContribucionEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.DetalleEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.EntregasEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ResumenEquipos;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.SnapshotEquipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S09 y S10: códigos HTTP, forma de las tres secciones y 401/403/503. */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, ConversorJwtKeycloak.class, ZonaHoraria.class})
class DashboardControllerTest {

    private static final String RUTA = "/api/seguimiento/dashboard/equipos";
    private static final Instant AHORA = Instant.parse("2026-11-19T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private DetalleEquipo resultado() {
        EquipoDto datos = new EquipoDto(7L, "Equipo 1", 10L, 2L, "ASEGURADO", true, "sub-a", List.of());
        return new DetalleEquipo(
                datos,
                new AsistenciaEquipo(2, 1, 1, List.of()),
                new EntregasEquipo(true, List.of(new EntregaEquipoDto(7L, 3L, "ENTREGADA", false, null))),
                new ActividadEquipo(true, List.of(new SnapshotEquipo(3L, EstadoSnapshot.ACTUALIZADO,
                        AHORA, 8, 2, false, List.of(new ContribucionEquipo("sub-a", "UsuarioA", 6, false))))));
    }

    @Test
    @DisplayName("S10: un docente recibe 200 con asistencia, entregas y actividad")
    void deberiaResponder200_conLasTresSecciones_cuandoElDocenteConsultaSuEquipo() throws Exception {
        when(servicio.dashboardDeEquipo(7L)).thenReturn(resultado());

        mockMvc.perform(get(RUTA + "/7").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipoId").value(7))
                .andExpect(jsonPath("$.asistencia.eventos").value(2))
                .andExpect(jsonPath("$.asistencia.presentes").value(1))
                .andExpect(jsonPath("$.entregas.disponible").value(true))
                .andExpect(jsonPath("$.entregas.hitos[0].estado").value("ENTREGADA"))
                .andExpect(jsonPath("$.actividad.snapshots[0].estado").value("ACTUALIZADO"))
                .andExpect(jsonPath("$.actividad.snapshots[0].integrantes[0].commits").value(6));
    }

    @Test
    @DisplayName("RN-33: S09 responde 200 con entregas.disponible=false si falla ms-documentacion")
    void deberiaResponder200Degradado_cuandoDocumentacionNoResponde() throws Exception {
        when(servicio.dashboardDeEquipos(10L, null)).thenReturn(new ResumenEquipos(
                10L, null, new EntregasEquipo(false, List.of()), List.of()));

        mockMvc.perform(get(RUTA).param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comisionLectivaId").value(10))
                .andExpect(jsonPath("$.entregas.disponible").value(false))
                .andExpect(jsonPath("$.equipos").isArray());
    }

    @ParameterizedTest(name = "sin token en {0} responde 401")
    @ValueSource(strings = {RUTA, RUTA + "/7"})
    @DisplayName("S09 y S10: sin token responde 401 y no llega al servicio")
    void deberiaResponder401_cuandoNoHayToken(String ruta) throws Exception {
        mockMvc.perform(get(ruta).param("comisionLectivaId", "10"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @ParameterizedTest(name = "un alumno en {0} responde 403")
    @ValueSource(strings = {RUTA, RUTA + "/7"})
    @DisplayName("S09 y S10: un alumno responde 403 y no llega al servicio")
    void deberiaResponder403_cuandoElRolEsAlumno(String ruta) throws Exception {
        mockMvc.perform(get(ruta).param("comisionLectivaId", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión responde 403 con la regla en el ProblemDetail")
    void deberiaResponder403_cuandoElDocenteEsDeOtraComision() throws Exception {
        when(servicio.dashboardDeEquipo(7L))
                .thenThrow(new AccesoDenegadoException("Docente de otra comisión", "RN-01"));

        mockMvc.perform(get(RUTA + "/7").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.regla").value("RN-01"));
    }

    @Test
    @DisplayName("Si ms-catedra no responde, S10 responde 503 con ProblemDetail")
    void deberiaResponder503_cuandoCatedraNoResponde() throws Exception {
        when(servicio.dashboardDeEquipo(7L))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra no responde"));

        mockMvc.perform(get(RUTA + "/7").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    @DisplayName("S09: sin filtros responde 400 con ProblemDetail")
    void deberiaResponder400_cuandoFaltaElFiltro() throws Exception {
        when(servicio.dashboardDeEquipos(null, null))
                .thenThrow(new SolicitudInvalidaException("Debe indicar comisionLectivaId o cursoLectivoId"));

        mockMvc.perform(get(RUTA).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_JEFE_CATEDRA"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
