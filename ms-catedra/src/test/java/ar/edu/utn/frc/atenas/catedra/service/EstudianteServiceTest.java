package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.catedra.dto.ActualizarUsuarioGithubRequest;
import ar.edu.utn.frc.atenas.catedra.dto.CrearEstudianteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteResponse;
import ar.edu.utn.frc.atenas.catedra.dto.MiPerfilResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.entity.Estudiante;
import ar.edu.utn.frc.atenas.catedra.entity.Integrante;
import ar.edu.utn.frc.atenas.catedra.estado.EstadoEquipo;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EquipoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EstudianteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.IntegranteRepository;
import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    private static final Long ESTUDIANTE_ID = 1L;
    private static final Long COMISION_LECTIVA_ID = 10L;
    private static final String LEGAJO = "411626";
    private static final String MAIL = LEGAJO + "@sistemas.frc.utn.edu.ar";
    private static final String SUB = "sub-alumno";

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private ComisionLectivaRepository comisionLectivaRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private IntegranteRepository integranteRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private EquipoService equipoService;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KeycloakProperties keycloakProperties;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private EstudianteService estudianteService;

    @Test
    @DisplayName("RN-02/RN-03: crea estudiante, deriva el mail y asigna el rol ALUMNO")
    void deberiaCrearEstudiante_cuandoDatosValidos() {
        when(estudianteRepository.existsByLegajo(LEGAJO)).thenReturn(false);
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID))
                .thenReturn(Optional.of(comisionLectiva()));
        when(keycloakProperties.getClaveTemporal()).thenReturn("clave-temporal");
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SUB);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(invocacion -> {
            Estudiante estudiante = invocacion.getArgument(0);
            if (estudiante.getId() == null) {
                estudiante.setId(ESTUDIANTE_ID);
            }
            return estudiante;
        });

        EstudianteResponse respuesta = estudianteService.crear(
                new CrearEstudianteRequest(LEGAJO, "Alumno", "Prueba", COMISION_LECTIVA_ID));

        assertEquals(ESTUDIANTE_ID, respuesta.id());
        assertEquals(MAIL, respuesta.mail());
        assertEquals(LEGAJO, respuesta.legajo());
        verify(keycloakAdminClient).asignarRolRealm(SUB, Roles.ALUMNO);
    }

    @Test
    @DisplayName("RN-02: rechaza un legajo con caracteres no numéricos con 400")
    void deberiaRechazarLegajo_cuandoNoEsNumerico() {
        assertThrows(SolicitudInvalidaException.class,
                () -> estudianteService.crear(
                        new CrearEstudianteRequest("41A626", "Alumno", "Prueba", COMISION_LECTIVA_ID)));
        verify(estudianteRepository, never()).existsByLegajo(anyString());
    }

    @Test
    @DisplayName("RN-02: rechaza un legajo duplicado con 409")
    void deberiaRechazarLegajo_cuandoYaExiste() {
        when(estudianteRepository.existsByLegajo(LEGAJO)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> estudianteService.crear(
                        new CrearEstudianteRequest(LEGAJO, "Alumno", "Prueba", COMISION_LECTIVA_ID)));
        verify(keycloakAdminClient, never()).crearUsuario(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("RN-03: propaga el 503 cuando Keycloak falla en el alta")
    void deberiaPropagarServicioNoDisponible_cuandoKeycloakFallaEnAlta() {
        when(estudianteRepository.existsByLegajo(LEGAJO)).thenReturn(false);
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID))
                .thenReturn(Optional.of(comisionLectiva()));
        when(keycloakProperties.getClaveTemporal()).thenReturn("clave-temporal");
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("Keycloak caído"));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(invocacion -> {
            Estudiante estudiante = invocacion.getArgument(0);
            estudiante.setId(ESTUDIANTE_ID);
            return estudiante;
        });

        assertThrows(ServicioNoDisponibleException.class,
                () -> estudianteService.crear(
                        new CrearEstudianteRequest(LEGAJO, "Alumno", "Prueba", COMISION_LECTIVA_ID)));
    }

    @Test
    @DisplayName("C12: el jefe ve todos los estudiantes de la comisión")
    void deberiaListarTodos_cuandoEsJefe() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(estudianteRepository.findByComisionLectivaId(COMISION_LECTIVA_ID))
                .thenReturn(List.of(estudiante()));

        List<EstudianteResponse> respuesta = estudianteService.listarPorComision(COMISION_LECTIVA_ID);

        assertEquals(1, respuesta.size());
        assertEquals(LEGAJO, respuesta.getFirst().legajo());
    }

    @Test
    @DisplayName("RN-01: el docente asignado ve los estudiantes de su comisión")
    void deberiaListarEstudiantes_cuandoDocenteEstaAsignado() {
        prepararDocente();
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(3L, COMISION_LECTIVA_ID))
                .thenReturn(true);
        when(estudianteRepository.findByComisionLectivaId(COMISION_LECTIVA_ID))
                .thenReturn(List.of(estudiante()));

        List<EstudianteResponse> respuesta = estudianteService.listarPorComision(COMISION_LECTIVA_ID);

        assertEquals(1, respuesta.size());
    }

    @Test
    @DisplayName("RN-01: el docente no asignado recibe 403")
    void deberiaLanzarAccesoDenegado_cuandoDocenteNoEstaAsignado() {
        prepararDocente();
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(3L, COMISION_LECTIVA_ID))
                .thenReturn(false);

        assertThrows(AccesoDenegadoException.class,
                () -> estudianteService.listarPorComision(COMISION_LECTIVA_ID));
    }

    @Test
    @DisplayName("C12: responde 404 cuando la comisión no existe")
    void deberiaLanzarNoEncontrado_cuandoComisionNoExisteEnC12() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(false);

        assertThrows(NoEncontradoException.class,
                () -> estudianteService.listarPorComision(COMISION_LECTIVA_ID));
    }

    @Test
    @DisplayName("RN-05: responde 404 cuando el estudiante no existe")
    void deberiaLanzarNoEncontrado_cuandoEstudianteNoExiste() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> estudianteService.darDeBaja(99L));
    }

    @Test
    @DisplayName("RN-05: da de baja y deshabilita la cuenta")
    void deberiaDarDeBaja_cuandoKeycloakResponde() {
        when(estudianteRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante()));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EstudianteResponse respuesta = estudianteService.darDeBaja(ESTUDIANTE_ID);

        assertFalse(respuesta.activo());
        verify(keycloakAdminClient).deshabilitarUsuario(SUB);
    }

    @Test
    @DisplayName("RN-05: propaga el 503 cuando Keycloak falla en la baja")
    void deberiaPropagarServicioNoDisponible_cuandoKeycloakFallaEnBaja() {
        when(estudianteRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante()));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        doThrow(new ServicioNoDisponibleException("Keycloak caído"))
                .when(keycloakAdminClient).deshabilitarUsuario(SUB);

        assertThrows(ServicioNoDisponibleException.class,
                () -> estudianteService.darDeBaja(ESTUDIANTE_ID));
    }

    @Test
    @DisplayName("RN-26: guarda el usuario de GitHub en minúsculas")
    void deberiaGuardarUsuarioGithubEnMinusculas() {
        Estudiante estudiante = estudiante();
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.existsByUsuarioGithub("alumno-uno")).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        MiPerfilResponse respuesta = estudianteService.actualizarUsuarioGithub(
                new ActualizarUsuarioGithubRequest("Alumno-Uno"));

        assertEquals("alumno-uno", respuesta.usuarioGithub());
    }

    @Test
    @DisplayName("RN-26: rechaza un usuario de GitHub duplicado con 409")
    void deberiaRechazarUsuarioGithubDuplicado() {
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante()));
        when(estudianteRepository.existsByUsuarioGithub("alumno-uno")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> estudianteService.actualizarUsuarioGithub(
                        new ActualizarUsuarioGithubRequest("alumno-uno")));
    }

    @Test
    @DisplayName("RN-26: rechaza un formato inválido de GitHub con 400")
    void deberiaRechazarUsuarioGithubInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> estudianteService.actualizarUsuarioGithub(
                        new ActualizarUsuarioGithubRequest("usuario_con_guion_bajo")));
    }

    @Test
    @DisplayName("C14: devuelve comisión, curso y equipo nulo")
    void deberiaDevolverMiPerfil() {
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante()));

        MiPerfilResponse respuesta = estudianteService.miPerfil();

        assertEquals(LEGAJO, respuesta.legajo());
        assertEquals("Comisión 1", respuesta.comisionNombre());
        assertEquals(2026, respuesta.cursoAnio());
        assertNull(respuesta.equipoId());
    }

    @Test
    @DisplayName("C14: devuelve el equipo vigente cuando el alumno ya ingresó")
    void deberiaDevolverEquipoVigenteEnMiPerfil() {
        Estudiante estudiante = estudiante();
        Integrante integrante = new Integrante(
                30L,
                new Equipo(20L, "Equipo 1", 2, 3, EstadoEquipo.ABIERTO, comisionLectiva(), null),
                estudiante,
                Instant.now(),
                null);
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante));
        when(integranteRepository.findByEstudianteIdAndFechaBajaIsNull(ESTUDIANTE_ID))
                .thenReturn(Optional.of(integrante));

        MiPerfilResponse respuesta = estudianteService.miPerfil();

        assertEquals(20L, respuesta.equipoId());
    }

    @Test
    @DisplayName("C14: responde 404 si no existe el estudiante del token")
    void deberiaLanzarNoEncontrado_cuandoNoHayPerfil() {
        when(usuarioActual.sub()).thenReturn("sub-desconocido");
        when(estudianteRepository.findBySub("sub-desconocido")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> estudianteService.miPerfil());
    }

    @Test
    @DisplayName("I03: devuelve pertenencia con el equipo vigente")
    void deberiaDevolverContratoInterno() {
        Estudiante estudiante = estudiante();
        Integrante integrante = new Integrante(
                30L,
                new Equipo(20L, "Equipo 1", 2, 3, EstadoEquipo.ABIERTO, comisionLectiva(), null),
                estudiante,
                Instant.now(),
                null);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante));
        when(integranteRepository.findByEstudianteIdAndFechaBajaIsNull(ESTUDIANTE_ID))
                .thenReturn(Optional.of(integrante));

        EstudianteInternoResponse respuesta = estudianteService.obtenerInternoPorSub(SUB);

        assertEquals(ESTUDIANTE_ID, respuesta.estudianteId());
        assertEquals(SUB, respuesta.sub());
        assertEquals(COMISION_LECTIVA_ID, respuesta.comisionLectivaId());
        assertEquals(20L, respuesta.equipoId());
    }

    @Test
    @DisplayName("RN-05: cierra la participación cuando el equipo está ABIERTO")
    void deberiaCerrarParticipacion_cuandoTieneEquipoAbierto() {
        Integrante integrante = integrante(equipoAbierto());
        when(estudianteRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante()));
        when(integranteRepository.findByEstudianteIdAndFechaBajaIsNull(ESTUDIANTE_ID))
                .thenReturn(Optional.of(integrante));
        when(equipoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(equipoAbierto()));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EstudianteResponse respuesta = estudianteService.darDeBaja(ESTUDIANTE_ID);

        assertFalse(respuesta.activo());
        verify(equipoService).cerrarParticipacion(any(Equipo.class), same(integrante));
        verify(keycloakAdminClient).deshabilitarUsuario(SUB);
    }

    @Test
    @DisplayName("RN-05: rechaza la baja si el equipo está ASEGURADO")
    void deberiaRechazarBaja_cuandoElEquipoEstaAsegurado() {
        Integrante integrante = integrante(equipoAsegurado());
        when(estudianteRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante()));
        when(integranteRepository.findByEstudianteIdAndFechaBajaIsNull(ESTUDIANTE_ID))
                .thenReturn(Optional.of(integrante));
        when(equipoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(equipoAsegurado()));
        doThrow(new ConflictoException("El equipo está asegurado", "RN-05"))
                .when(equipoService).cerrarParticipacion(any(Equipo.class), same(integrante));

        assertThrows(ConflictoException.class, () -> estudianteService.darDeBaja(ESTUDIANTE_ID));

        verify(estudianteRepository, never()).saveAndFlush(any(Estudiante.class));
        verify(keycloakAdminClient, never()).deshabilitarUsuario(SUB);
    }

    private void prepararDocente() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente"))
                .thenReturn(Optional.of(new Docente(3L, "Docente", "Prueba", "docente@atenas.local", "sub-docente", true)));
    }

    private Estudiante estudiante() {
        return new Estudiante(
                ESTUDIANTE_ID,
                LEGAJO,
                "Alumno",
                "Prueba",
                MAIL,
                SUB,
                null,
                comisionLectiva(),
                true);
    }

    private ComisionLectiva comisionLectiva() {
        return new ComisionLectiva(
                COMISION_LECTIVA_ID,
                new Comision(5L, "Comisión 1"),
                new CursoLectivo(2L, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
    }

    private Equipo equipoAbierto() {
        return new Equipo(20L, "Equipo 1", 2, 3, EstadoEquipo.ABIERTO, comisionLectiva(), estudiante());
    }

    private Equipo equipoAsegurado() {
        return new Equipo(20L, "Equipo 1", 2, 3, EstadoEquipo.ASEGURADO, comisionLectiva(), estudiante());
    }

    private Integrante integrante(Equipo equipo) {
        return new Integrante(30L, equipo, estudiante(), Instant.now(), null);
    }
}
