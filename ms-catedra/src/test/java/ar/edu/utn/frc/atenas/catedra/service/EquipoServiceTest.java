package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.CrearEquipoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResumenResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    private static final Long COMISION_LECTIVA_ID = 10L;
    private static final Long EQUIPO_ID = 20L;
    private static final Long DOCENTE_ID = 3L;
    private static final String SUB = "sub-alumno";

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private IntegranteRepository integranteRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private ComisionLectivaRepository comisionLectivaRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private EquipoService equipoService;

    @Test
    @DisplayName("RN-06: rechaza cupo mínimo menor a 1")
    void deberiaRechazarCupo_cuandoMinimoEsMenorAUno() {
        prepararDocenteAsignado();

        assertThrows(SolicitudInvalidaException.class,
                () -> equipoService.crear(COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 0, 2)));
    }

    @Test
    @DisplayName("RN-06: rechaza cupo mínimo mayor al máximo")
    void deberiaRechazarCupo_cuandoMinimoSuperaMaximo() {
        prepararDocenteAsignado();

        assertThrows(SolicitudInvalidaException.class,
                () -> equipoService.crear(COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 4, 3)));
    }

    @Test
    @DisplayName("C16: rechaza un nombre duplicado en la misma comisión")
    void deberiaRechazarEquipo_cuandoNombreDuplicado() {
        prepararDocenteAsignado();
        when(equipoRepository.existsByComisionLectivaIdAndNombre(COMISION_LECTIVA_ID, "Equipo")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> equipoService.crear(COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 2, 3)));
        verify(equipoRepository, never()).saveAndFlush(any(Equipo.class));
    }

    @Test
    @DisplayName("C16: crea el equipo ABIERTO y sin representante")
    void deberiaCrearEquipo_cuandoDatosValidos() {
        prepararDocenteAsignado();
        when(equipoRepository.existsByComisionLectivaIdAndNombre(COMISION_LECTIVA_ID, "Equipo")).thenReturn(false);
        when(equipoRepository.saveAndFlush(any(Equipo.class))).thenAnswer(invocacion -> {
            Equipo equipo = invocacion.getArgument(0);
            equipo.setId(EQUIPO_ID);
            return equipo;
        });

        EquipoResponse respuesta = equipoService.crear(
                COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 2, 3));

        assertEquals(EQUIPO_ID, respuesta.id());
        assertEquals("ABIERTO", respuesta.estado());
        assertFalse(respuesta.asegurado());
        assertNull(respuesta.representanteSub());
    }

    @Test
    @DisplayName("C16: responde 403 si el docente no está asignado")
    void deberiaRechazarCreacion_cuandoDocenteNoEstaAsignado() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID))
                .thenReturn(Optional.of(comisionLectiva()));
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(false);

        assertThrows(AccesoDenegadoException.class,
                () -> equipoService.crear(COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 2, 3)));
    }

    @Test
    @DisplayName("C16: responde 404 si la comisión no existe")
    void deberiaRechazarCreacion_cuandoComisionNoExiste() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> equipoService.crear(COMISION_LECTIVA_ID, new CrearEquipoRequest("Equipo", 2, 3)));
    }

    @Test
    @DisplayName("C17: el jefe ve equipos y disponibilidad")
    void deberiaListarEquipos_cuandoEsJefe() {
        Equipo equipo = equipoAbierto();
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(equipoRepository.findByComisionLectivaId(COMISION_LECTIVA_ID)).thenReturn(List.of(equipo));
        when(integranteRepository.findByEquipoIdInAndFechaBajaIsNull(List.of(EQUIPO_ID)))
                .thenReturn(List.of(integrante(equipo, estudiante())));

        List<EquipoResumenResponse> respuesta = equipoService.listarPorComision(COMISION_LECTIVA_ID);

        assertEquals(1, respuesta.size());
        assertEquals(1, respuesta.getFirst().cantidadIntegrantes());
        assertTrue(respuesta.getFirst().disponible());
    }

    @Test
    @DisplayName("RN-01: el docente asignado puede listar los equipos de su comisión")
    void deberiaListarEquipos_cuandoDocenteEstaAsignado() {
        Equipo equipo = equipoAbierto();
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);
        when(equipoRepository.findByComisionLectivaId(COMISION_LECTIVA_ID)).thenReturn(List.of(equipo));
        when(integranteRepository.findByEquipoIdInAndFechaBajaIsNull(List.of(EQUIPO_ID)))
                .thenReturn(List.of());

        List<EquipoResumenResponse> respuesta = equipoService.listarPorComision(COMISION_LECTIVA_ID);

        assertEquals(1, respuesta.size());
    }

    @Test
    @DisplayName("RN-01: un alumno de otra comisión no puede listar equipos")
    void deberiaRechazarListado_cuandoAlumnoEsDeOtraComision() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB))
                .thenReturn(Optional.of(estudianteEnComision(11L)));

        assertThrows(AccesoDenegadoException.class,
                () -> equipoService.listarPorComision(COMISION_LECTIVA_ID));
    }

    @Test
    @DisplayName("C18: responde 404 si el equipo no existe")
    void deberiaRechazarConsulta_cuandoEquipoNoExiste() {
        when(equipoRepository.buscarPorId(EQUIPO_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> equipoService.obtener(EQUIPO_ID));
    }

    @Test
    @DisplayName("C18: devuelve integrantes y representante")
    void deberiaObtenerEquipo_cuandoEsJefe() {
        Equipo equipo = equipoAbierto();
        equipo.setRepresentante(estudiante());
        when(equipoRepository.buscarPorId(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID))
                .thenReturn(List.of(integrante(equipo, estudiante())));

        EquipoResponse respuesta = equipoService.obtener(EQUIPO_ID);

        assertEquals(SUB, respuesta.representanteSub());
        assertEquals(1, respuesta.integrantes().size());
    }

    @Test
    @DisplayName("RN-07: rechaza el ingreso si el alumno ya integra un equipo")
    void deberiaRechazarIngreso_cuandoYaTieneEquipo() {
        prepararIngreso(equipoAbierto(), estudiante());
        when(integranteRepository.existsByEstudianteIdAndFechaBajaIsNull(1L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> equipoService.ingresar(EQUIPO_ID));
        verify(integranteRepository, never()).saveAndFlush(any(Integrante.class));
    }

    @Test
    @DisplayName("RN-08: rechaza el ingreso si el equipo está ASEGURADO")
    void deberiaRechazarIngreso_cuandoEquipoEstaAsegurado() {
        prepararIngreso(equipoAsegurado(), estudiante());
        when(integranteRepository.existsByEstudianteIdAndFechaBajaIsNull(1L)).thenReturn(false);

        ConflictoException excepcion = assertThrows(ConflictoException.class,
                () -> equipoService.ingresar(EQUIPO_ID));
        assertEquals("RN-08", excepcion.getRegla());
    }

    @Test
    @DisplayName("RN-08: rechaza el ingreso si no hay cupo")
    void deberiaRechazarIngreso_cuandoNoHayCupo() {
        prepararIngreso(equipoAbierto(), estudiante());
        when(integranteRepository.existsByEstudianteIdAndFechaBajaIsNull(1L)).thenReturn(false);
        when(integranteRepository.countByEquipoIdAndFechaBajaIsNull(EQUIPO_ID)).thenReturn(3L);

        assertThrows(ConflictoException.class, () -> equipoService.ingresar(EQUIPO_ID));
    }

    @Test
    @DisplayName("RN-09: el primer ingresante queda como representante")
    void deberiaAsignarRepresentante_cuandoEsElPrimerIngreso() {
        Equipo equipo = equipoAbierto();
        Estudiante estudiante = estudiante();
        prepararIngreso(equipo, estudiante);
        when(integranteRepository.existsByEstudianteIdAndFechaBajaIsNull(1L)).thenReturn(false);
        when(integranteRepository.countByEquipoIdAndFechaBajaIsNull(EQUIPO_ID)).thenReturn(0L);
        when(integranteRepository.saveAndFlush(any(Integrante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(equipoRepository.saveAndFlush(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID))
                .thenReturn(List.of(integrante(equipo, estudiante)));

        EquipoResponse respuesta = equipoService.ingresar(EQUIPO_ID);

        assertEquals(SUB, respuesta.representanteSub());
        verify(equipoRepository).saveAndFlush(equipo);
    }

    @Test
    @DisplayName("C19: rechaza el ingreso de un alumno de otra comisión")
    void deberiaRechazarIngreso_cuandoAlumnoEsDeOtraComision() {
        prepararIngreso(equipoAbierto(), estudianteEnComision(11L));

        assertThrows(AccesoDenegadoException.class, () -> equipoService.ingresar(EQUIPO_ID));
    }

    @Test
    @DisplayName("C19: responde 404 si no existe el estudiante del token")
    void deberiaRechazarIngreso_cuandoNoExisteEstudiante() {
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipoAbierto()));
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> equipoService.ingresar(EQUIPO_ID));
    }

    @Test
    @DisplayName("I02: exige exactamente un filtro")
    void deberiaRechazarConsultaInterna_cuandoNoHayUnSoloFiltro() {
        assertThrows(SolicitudInvalidaException.class,
                () -> equipoService.listarInterno(null, null));
        assertThrows(SolicitudInvalidaException.class,
                () -> equipoService.listarInterno(COMISION_LECTIVA_ID, 2L));
    }

    @Test
    @DisplayName("I01: devuelve el formato interno del equipo")
    void deberiaDevolverEquipoInterno() {
        Equipo equipo = equipoAbierto();
        equipo.setRepresentante(estudiante());
        when(equipoRepository.buscarPorId(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID))
                .thenReturn(List.of(integrante(equipo, estudiante())));

        EquipoInternoResponse respuesta = equipoService.obtenerInterno(EQUIPO_ID);

        assertEquals(EQUIPO_ID, respuesta.id());
        assertEquals(COMISION_LECTIVA_ID, respuesta.comisionLectivaId());
        assertEquals(SUB, respuesta.representanteSub());
        assertEquals(1, respuesta.integrantes().size());
    }

    @Test
    @DisplayName("I02: devuelve la lista de equipos de una comisión")
    void deberiaListarEquiposInternos() {
        Equipo equipo = equipoAbierto();
        when(equipoRepository.findByComisionLectivaId(COMISION_LECTIVA_ID)).thenReturn(List.of(equipo));
        when(integranteRepository.findByEquipoIdInAndFechaBajaIsNull(List.of(EQUIPO_ID)))
                .thenReturn(List.of(integrante(equipo, estudiante())));

        List<EquipoInternoResponse> respuesta = equipoService.listarInterno(COMISION_LECTIVA_ID, null);

        assertEquals(1, respuesta.size());
        assertEquals(1, respuesta.getFirst().integrantes().size());
    }

    @Test
    @DisplayName("I02: devuelve la lista de equipos de un curso")
    void deberiaListarEquiposInternosPorCurso() {
        Equipo equipo = equipoAbierto();
        when(equipoRepository.findByComisionLectivaCursoLectivoId(2L)).thenReturn(List.of(equipo));
        when(integranteRepository.findByEquipoIdInAndFechaBajaIsNull(List.of(EQUIPO_ID)))
                .thenReturn(List.of());

        List<EquipoInternoResponse> respuesta = equipoService.listarInterno(null, 2L);

        assertEquals(1, respuesta.size());
    }

    @Test
    @DisplayName("RN-09: el propio integrante puede salir y se actualiza el representante")
    void deberiaSalir_cuandoEsElPropioIntegrante() {
        Equipo equipo = equipoAbierto();
        Estudiante estudiante = estudiante();
        equipo.setRepresentante(estudiante);
        Integrante integrante = integrante(equipo, estudiante);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(integranteRepository.findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(EQUIPO_ID, 1L))
                .thenReturn(Optional.of(integrante));
        when(usuarioActual.sub()).thenReturn(SUB);
        when(integranteRepository.saveAndFlush(any(Integrante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNullOrderByFechaIngresoAscIdAsc(EQUIPO_ID))
                .thenReturn(List.of());
        when(equipoRepository.saveAndFlush(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        equipoService.salir(EQUIPO_ID, 1L);

        assertNull(equipo.getRepresentante());
        verify(integranteRepository).saveAndFlush(integrante);
    }

    @Test
    @DisplayName("RN-09: el representante puede rechazar a otro integrante")
    void deberiaRechazarIngreso_cuandoEsElRepresentante() {
        Equipo equipo = equipoAbierto();
        Estudiante representante = estudiante();
        Estudiante otro = estudianteOtro();
        equipo.setRepresentante(representante);
        Integrante integrante = integrante(equipo, otro);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(integranteRepository.findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(EQUIPO_ID, 2L))
                .thenReturn(Optional.of(integrante));
        when(usuarioActual.sub()).thenReturn(SUB);
        when(integranteRepository.saveAndFlush(any(Integrante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        equipoService.salir(EQUIPO_ID, 2L);

        assertEquals(representante, equipo.getRepresentante());
        verify(equipoRepository, never()).saveAndFlush(any(Equipo.class));
    }

    @Test
    @DisplayName("RN-09: otro integrante no puede rechazar a un compañero")
    void deberiaRechazarSalida_cuandoNoEsPropioNiRepresentante() {
        Equipo equipo = equipoAbierto();
        Estudiante representante = estudiante();
        Estudiante otro = estudianteOtro();
        equipo.setRepresentante(representante);
        Integrante integrante = integrante(equipo, otro);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(integranteRepository.findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(EQUIPO_ID, 2L))
                .thenReturn(Optional.of(integrante));
        when(usuarioActual.sub()).thenReturn("sub-tercero");

        assertThrows(AccesoDenegadoException.class, () -> equipoService.salir(EQUIPO_ID, 2L));
        verify(integranteRepository, never()).saveAndFlush(any(Integrante.class));
    }

    @Test
    @DisplayName("RN-09: el equipo ASEGURADO rechaza la salida")
    void deberiaRechazarSalida_cuandoEquipoEstaAsegurado() {
        Equipo equipo = equipoAsegurado();
        Estudiante estudiante = estudiante();
        equipo.setRepresentante(estudiante);
        Integrante integrante = integrante(equipo, estudiante);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(integranteRepository.findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(EQUIPO_ID, 1L))
                .thenReturn(Optional.of(integrante));
        when(usuarioActual.sub()).thenReturn(SUB);

        assertThrows(ConflictoException.class, () -> equipoService.salir(EQUIPO_ID, 1L));
        verify(integranteRepository, never()).saveAndFlush(any(Integrante.class));
    }

    @Test
    @DisplayName("RN-10: el docente asignado puede asegurar sin condiciones")
    void deberiaAsegurar_cuandoEsDocenteAsignado() {
        Equipo equipo = equipoAbierto();
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);
        when(equipoRepository.saveAndFlush(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID)).thenReturn(List.of());

        EquipoResponse respuesta = equipoService.asegurar(EQUIPO_ID);

        assertTrue(respuesta.asegurado());
        assertEquals("ASEGURADO", respuesta.estado());
    }

    @Test
    @DisplayName("RN-10: el representante asegura con cupo y GitHub de todos")
    void deberiaAsegurar_cuandoEsRepresentanteConCondiciones() {
        Equipo equipo = equipoAbierto();
        Estudiante representante = estudiante();
        equipo.setRepresentante(representante);
        List<Integrante> integrantes = List.of(
                integrante(equipo, representante),
                integrante(equipo, estudianteOtro()));

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn(SUB);
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID)).thenReturn(integrantes);
        when(equipoRepository.saveAndFlush(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EquipoResponse respuesta = equipoService.asegurar(EQUIPO_ID);

        assertTrue(respuesta.asegurado());
    }

    @Test
    @DisplayName("RN-10: el representante no puede asegurar si falta GitHub")
    void deberiaRechazarAseguramiento_cuandoFaltaGithub() {
        Equipo equipo = equipoAbierto();
        Estudiante representante = estudiante();
        equipo.setRepresentante(representante);
        Estudiante sinGithub = new Estudiante(
                2L, "411627", "Otro", "Alumno", "411627@sistemas.frc.utn.edu.ar",
                "sub-otro", null, comisionLectiva(), true);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn(SUB);
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID))
                .thenReturn(List.of(integrante(equipo, representante), integrante(equipo, sinGithub)));

        assertThrows(ConflictoException.class, () -> equipoService.asegurar(EQUIPO_ID));
    }

    @Test
    @DisplayName("RN-10: el representante no puede asegurar fuera del rango de cupo")
    void deberiaRechazarAseguramiento_cuandoNoHayCupoMinimo() {
        Equipo equipo = equipoAbierto();
        Estudiante representante = estudiante();
        equipo.setRepresentante(representante);

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn(SUB);
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID))
                .thenReturn(List.of(integrante(equipo, representante)));

        assertThrows(ConflictoException.class, () -> equipoService.asegurar(EQUIPO_ID));
    }

    @Test
    @DisplayName("RN-10: un alumno no representante no puede asegurar")
    void deberiaRechazarAseguramiento_cuandoNoEsRepresentante() {
        Equipo equipo = equipoAbierto();
        equipo.setRepresentante(estudiante());

        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-otro");

        assertThrows(AccesoDenegadoException.class, () -> equipoService.asegurar(EQUIPO_ID));
    }

    @Test
    @DisplayName("RN-11: el docente asignado puede reabrir un equipo asegurado")
    void deberiaReabrir_cuandoEsDocenteAsignado() {
        Equipo equipo = equipoAsegurado();
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);
        when(equipoRepository.saveAndFlush(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(integranteRepository.findByEquipoIdAndFechaBajaIsNull(EQUIPO_ID)).thenReturn(List.of());

        EquipoResponse respuesta = equipoService.reabrir(EQUIPO_ID);

        assertFalse(respuesta.asegurado());
        assertEquals("ABIERTO", respuesta.estado());
    }

    @Test
    @DisplayName("RN-11: un equipo ya abierto no se puede reabrir")
    void deberiaRechazarReapertura_cuandoYaEstaAbierto() {
        Equipo equipo = equipoAbierto();
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);

        assertThrows(ConflictoException.class, () -> equipoService.reabrir(EQUIPO_ID));
    }

    @Test
    @DisplayName("RN-11: un docente no asignado no puede reabrir")
    void deberiaRechazarReapertura_cuandoDocenteNoEstaAsignado() {
        Equipo equipo = equipoAsegurado();
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(false);

        assertThrows(AccesoDenegadoException.class, () -> equipoService.reabrir(EQUIPO_ID));
    }

    private void prepararDocenteAsignado() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID))
                .thenReturn(Optional.of(comisionLectiva()));
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);
    }

    private void prepararIngreso(Equipo equipo, Estudiante estudiante) {
        when(equipoRepository.findByIdForUpdate(EQUIPO_ID)).thenReturn(Optional.of(equipo));
        when(usuarioActual.sub()).thenReturn(SUB);
        when(estudianteRepository.findBySub(SUB)).thenReturn(Optional.of(estudiante));
    }

    private ComisionLectiva comisionLectiva() {
        return new ComisionLectiva(
                COMISION_LECTIVA_ID,
                new Comision(5L, "Comisión 1"),
                new CursoLectivo(2L, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
    }

    private Equipo equipoAbierto() {
        return new Equipo(EQUIPO_ID, "Equipo 1", 2, 3, EstadoEquipo.ABIERTO, comisionLectiva(), null);
    }

    private Equipo equipoAsegurado() {
        return new Equipo(EQUIPO_ID, "Equipo 1", 2, 3, EstadoEquipo.ASEGURADO, comisionLectiva(), null);
    }

    private Docente docente() {
        return new Docente(DOCENTE_ID, "Docente", "Prueba", "docente@atenas.local", "sub-docente", true);
    }

    private Estudiante estudiante() {
        return estudianteEnComision(COMISION_LECTIVA_ID);
    }

    private Estudiante estudianteOtro() {
        return new Estudiante(
                2L,
                "411627",
                "Otro",
                "Alumno",
                "411627@sistemas.frc.utn.edu.ar",
                "sub-otro",
                "alumno-otro",
                comisionLectiva(),
                true);
    }

    private Estudiante estudianteEnComision(Long comisionLectivaId) {
        ComisionLectiva comision = comisionLectivaId.equals(COMISION_LECTIVA_ID)
                ? comisionLectiva()
                : new ComisionLectiva(
                        comisionLectivaId,
                        new Comision(6L, "Comisión 2"),
                        new CursoLectivo(2L, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
        return new Estudiante(
                1L,
                "411626",
                "Alumno",
                "Prueba",
                "411626@sistemas.frc.utn.edu.ar",
                SUB,
                "alumno",
                comision,
                true);
    }

    private Integrante integrante(Equipo equipo, Estudiante estudiante) {
        return new Integrante(30L, equipo, estudiante, Instant.now(), null);
    }
}
