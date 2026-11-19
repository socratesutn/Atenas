package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.repository.AsistenciaRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    private static final Long EVENTO_ID = 3L;
    private static final Long COMISION = 10L;
    private static final Long EQUIPO_ID = 7L;

    @Mock
    private EventoRepository eventos;
    @Mock
    private AsistenciaRepository asistencias;
    @Mock
    private CatedraClient catedraClient;
    @Mock
    private AccesoComision acceso;
    @Mock
    private UsuarioActual usuarioActual;

    private AsistenciaService servicio;

    @BeforeEach
    void preparar() {
        TransactionTemplate transaccion = new TransactionTemplate(mock(PlatformTransactionManager.class));
        servicio = new AsistenciaService(eventos, asistencias, catedraClient, acceso, usuarioActual, transaccion);
    }

    // ---------------------------------------------------------------- utilidades

    private static Evento eventoPasado() {
        return Evento.builder().id(EVENTO_ID).comisionLectivaId(COMISION).tipo(TipoEvento.CLASE)
                .fecha(Instant.now().minus(1, ChronoUnit.DAYS)).build();
    }

    private static Evento eventoFuturo() {
        return Evento.builder().id(EVENTO_ID).comisionLectivaId(COMISION).tipo(TipoEvento.CLASE)
                .fecha(Instant.now().plus(1, ChronoUnit.DAYS)).build();
    }

    private static EstudianteDto alumno(String sub, Long comision, Long equipoId) {
        return new EstudianteDto(5L, sub, true, comision, 2L, equipoId);
    }

    private static EquipoDto equipo(Long comision, String... subs) {
        List<EquipoDto.IntegranteDto> integrantes = java.util.Arrays.stream(subs)
                .map(sub -> new EquipoDto.IntegranteDto(1L, sub, "legajo", "usuario"))
                .toList();
        return new EquipoDto(EQUIPO_ID, "Equipo 1", comision, 2L, "ASEGURADO", true, subs.length > 0 ? subs[0] : null, integrantes);
    }

    private void eventoExiste(Evento evento) {
        when(eventos.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
    }

    private void guardarDevuelveLoRecibido() {
        when(asistencias.save(any(Asistencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Asistencia filaExistente(Evento evento, String sub, boolean presente) {
        return Asistencia.builder().id(50L).evento(evento).estudianteSub(sub).equipoId(null).presente(presente)
                .registradaPorSub("otro-docente").fechaRegistro(Instant.parse("2026-10-01T10:00:00Z")).build();
    }

    // ---------------------------------------------------------------- S03: alumno

    @Test
    @DisplayName("RN-13: registra la asistencia de un alumno con su equipo vigente y quién la cargó")
    void deberiaRegistrarLaAsistencia_cuandoTodoEsValido() {
        Evento evento = eventoPasado();
        eventoExiste(evento);
        when(catedraClient.obtenerEstudiantePorSub("sub-a")).thenReturn(alumno("sub-a", COMISION, EQUIPO_ID));
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(asistencias.findByEventoIdAndEstudianteSub(EVENTO_ID, "sub-a")).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();
        Instant antes = Instant.now();

        Asistencia guardada = servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true);

        assertThat(guardada.getEvento()).isSameAs(evento);
        assertThat(guardada.getEstudianteSub()).isEqualTo("sub-a");
        assertThat(guardada.getEquipoId()).isEqualTo(EQUIPO_ID);
        assertThat(guardada.isPresente()).isTrue();
        assertThat(guardada.getRegistradaPorSub()).isEqualTo("sub-doc");
        assertThat(guardada.getFechaRegistro()).isBetween(antes, Instant.now());
        verify(acceso).exigirDocenteAsignado(COMISION, "RN-13");
    }

    @Test
    @DisplayName("RN-13: un alumno sin equipo se registra con equipo nulo")
    void deberiaGuardarEquipoNulo_cuandoElAlumnoNoTieneEquipo() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEstudiantePorSub("sub-a")).thenReturn(alumno("sub-a", COMISION, null));
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(asistencias.findByEventoIdAndEstudianteSub(EVENTO_ID, "sub-a")).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();

        assertThat(servicio.registrarDeAlumno(EVENTO_ID, "sub-a", false).getEquipoId()).isNull();
    }

    @Test
    @DisplayName("RN-13: repetir la carga actualiza la fila existente en lugar de duplicarla")
    void deberiaActualizarLaFila_cuandoLaAsistenciaYaEstabaRegistrada() {
        Evento evento = eventoPasado();
        eventoExiste(evento);
        Asistencia existente = filaExistente(evento, "sub-a", false);
        when(catedraClient.obtenerEstudiantePorSub("sub-a")).thenReturn(alumno("sub-a", COMISION, EQUIPO_ID));
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(asistencias.findByEventoIdAndEstudianteSub(EVENTO_ID, "sub-a")).thenReturn(Optional.of(existente));
        guardarDevuelveLoRecibido();

        Asistencia guardada = servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true);

        assertThat(guardada).isSameAs(existente);
        assertThat(guardada.getId()).isEqualTo(50L);
        assertThat(guardada.isPresente()).isTrue();
        assertThat(guardada.getRegistradaPorSub()).isEqualTo("sub-doc");
        assertThat(guardada.getEquipoId()).isEqualTo(EQUIPO_ID);
        assertThat(guardada.getFechaRegistro()).isAfter(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    @DisplayName("RN-13: un evento inexistente responde 404 y no se consulta nada más")
    void deberiaLanzarNoEncontrado_cuandoElEventoNoExiste() {
        when(eventos.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        verifyNoInteractions(acceso, catedraClient, asistencias);
    }

    @Test
    @DisplayName("RN-13: un docente de otra comisión recibe 403 y no se guarda nada")
    void deberiaPropagarAccesoDenegado_cuandoElDocenteEsDeOtraComision() {
        eventoExiste(eventoPasado());
        doThrow(new AccesoDenegadoException("Otra comisión", "RN-13")).when(acceso)
                .exigirDocenteAsignado(COMISION, "RN-13");

        assertThrows(AccesoDenegadoException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        verifyNoInteractions(catedraClient);
        verify(asistencias, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: no se registra asistencia de un evento futuro (409)")
    void deberiaLanzarConflicto_cuandoElEventoEsFuturo() {
        eventoExiste(eventoFuturo());

        ConflictoException excepcion =
                assertThrows(ConflictoException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        assertThat(excepcion.getRegla()).isEqualTo("RN-13");
        verifyNoInteractions(catedraClient);
        verify(asistencias, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: un alumno de otra comisión recibe 403 y no se guarda nada")
    void deberiaLanzarAccesoDenegado_cuandoElAlumnoEsDeOtraComision() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEstudiantePorSub("sub-a")).thenReturn(alumno("sub-a", 99L, null));

        AccesoDenegadoException excepcion = assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        assertThat(excepcion.getRegla()).isEqualTo("RN-13");
        verify(asistencias, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: un alumno inexistente (404 de ms-catedra) se propaga y no se guarda nada")
    void deberiaPropagarNoEncontrado_cuandoElAlumnoNoExiste() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEstudiantePorSub("sub-a")).thenThrow(new NoEncontradoException("No existe el alumno"));

        assertThrows(NoEncontradoException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        verify(asistencias, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: si ms-catedra no responde al validar al docente, 503 y no se guarda nada")
    void deberiaPropagarServicioNoDisponible_cuandoFallaLaValidacionDelDocente() {
        eventoExiste(eventoPasado());
        doThrow(new ServicioNoDisponibleException("ms-catedra caído")).when(acceso)
                .exigirDocenteAsignado(COMISION, "RN-13");

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        verify(asistencias, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: si ms-catedra no responde al validar al alumno, 503 y no se guarda nada")
    void deberiaPropagarServicioNoDisponible_cuandoFallaLaValidacionDelAlumno() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEstudiantePorSub("sub-a"))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.registrarDeAlumno(EVENTO_ID, "sub-a", true));

        verify(asistencias, never()).save(any());
    }

    // ---------------------------------------------------------------- S04: equipo

    @Test
    @DisplayName("RN-13: la carga por equipo crea una fila por integrante con el equipo informado")
    void deberiaCrearUnaFilaPorIntegrante_cuandoSeRegistraUnEquipo() {
        Evento evento = eventoPasado();
        eventoExiste(evento);
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo(COMISION, "sub-b", "sub-a"));
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(asistencias.findByEventoIdAndEstudianteSubIn(any(), anyCollection())).thenReturn(List.of());
        when(asistencias.saveAll(anyCollection())).thenAnswer(i -> List.copyOf(i.getArgument(0)));

        List<Asistencia> guardadas = servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true);

        assertThat(guardadas).extracting(Asistencia::getEstudianteSub).containsExactly("sub-a", "sub-b");
        assertThat(guardadas).allMatch(a -> a.isPresente() && EQUIPO_ID.equals(a.getEquipoId())
                && "sub-doc".equals(a.getRegistradaPorSub()) && a.getEvento() == evento);
        verify(acceso).exigirDocenteAsignado(COMISION, "RN-13");
    }

    @Test
    @DisplayName("RN-13: la carga por equipo actualiza las filas existentes y crea las que faltan")
    void deberiaActualizarYCrear_cuandoAlgunosIntegrantesYaTenianAsistencia() {
        Evento evento = eventoPasado();
        eventoExiste(evento);
        Asistencia existente = filaExistente(evento, "sub-a", true);
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo(COMISION, "sub-a", "sub-b"));
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(asistencias.findByEventoIdAndEstudianteSubIn(any(), anyCollection())).thenReturn(List.of(existente));
        when(asistencias.saveAll(anyCollection())).thenAnswer(i -> List.copyOf(i.getArgument(0)));

        List<Asistencia> guardadas = servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, false);

        assertThat(guardadas).hasSize(2);
        Asistencia deA = guardadas.stream().filter(a -> a.getEstudianteSub().equals("sub-a")).findFirst().orElseThrow();
        assertThat(deA).isSameAs(existente);
        assertThat(deA.getId()).isEqualTo(50L);
        assertThat(deA.isPresente()).isFalse();
        Asistencia deB = guardadas.stream().filter(a -> a.getEstudianteSub().equals("sub-b")).findFirst().orElseThrow();
        assertThat(deB.getId()).isNull();
        assertThat(deB.isPresente()).isFalse();
    }

    @Test
    @DisplayName("RN-13: un equipo de otra comisión recibe 403 y no se guarda nada")
    void deberiaLanzarAccesoDenegado_cuandoElEquipoEsDeOtraComision() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo(99L, "sub-a"));

        assertThrows(AccesoDenegadoException.class, () -> servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true));

        verify(asistencias, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("RN-13: un equipo sin integrantes vigentes responde 409 y no se guarda nada")
    void deberiaLanzarConflicto_cuandoElEquipoNoTieneIntegrantes() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo(COMISION));

        assertThrows(ConflictoException.class, () -> servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true));

        verify(asistencias, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("RN-13: un equipo inexistente (404 de ms-catedra) se propaga")
    void deberiaPropagarNoEncontrado_cuandoElEquipoNoExiste() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenThrow(new NoEncontradoException("No existe el equipo"));

        assertThrows(NoEncontradoException.class, () -> servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true));
    }

    @Test
    @DisplayName("RN-13: no se registra la asistencia de un equipo en un evento futuro (409)")
    void deberiaLanzarConflicto_cuandoElEventoEsFuturoAlRegistrarUnEquipo() {
        eventoExiste(eventoFuturo());

        assertThrows(ConflictoException.class, () -> servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true));

        verifyNoInteractions(catedraClient);
    }

    @Test
    @DisplayName("RN-13: si ms-catedra no responde al obtener el equipo, 503 y no se guarda nada")
    void deberiaPropagarServicioNoDisponible_cuandoFallaLaConsultaDelEquipo() {
        eventoExiste(eventoPasado());
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.registrarDeEquipo(EVENTO_ID, EQUIPO_ID, true));

        verify(asistencias, never()).saveAll(anyCollection());
    }

    // ---------------------------------------------------------------- S05

    @Test
    @DisplayName("RN-01: el jefe o un docente de la comisión listan la asistencia del evento")
    void deberiaListarLaAsistenciaDelEvento_cuandoTieneAcceso() {
        Evento evento = eventoPasado();
        eventoExiste(evento);
        Asistencia fila = filaExistente(evento, "sub-a", true);
        when(asistencias.findByEventoIdOrderByEstudianteSubAsc(EVENTO_ID)).thenReturn(List.of(fila));

        assertThat(servicio.listarDeEvento(EVENTO_ID)).containsExactly(fila);

        verify(acceso).exigirJefeODocente(COMISION);
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión recibe 403 al listar la asistencia")
    void deberiaPropagarAccesoDenegado_alListarDeOtraComision() {
        eventoExiste(eventoPasado());
        doThrow(new AccesoDenegadoException("Otra comisión", "RN-01")).when(acceso).exigirJefeODocente(COMISION);

        assertThrows(AccesoDenegadoException.class, () -> servicio.listarDeEvento(EVENTO_ID));

        verify(asistencias, never()).findByEventoIdOrderByEstudianteSubAsc(any());
    }

    @Test
    @DisplayName("Listar la asistencia de un evento inexistente responde 404")
    void deberiaLanzarNoEncontrado_alListarUnEventoInexistente() {
        when(eventos.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.listarDeEvento(EVENTO_ID));
    }

    // ---------------------------------------------------------------- S06

    @Test
    @DisplayName("RN-15: el alumno ve solo su asistencia, buscada por el sub del token")
    void deberiaBuscarLaAsistencia_porElSubDelToken() {
        Asistencia fila = filaExistente(eventoPasado(), "sub-alumno", true);
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(asistencias.findDeEstudiante("sub-alumno")).thenReturn(List.of(fila));

        assertThat(servicio.miAsistencia()).containsExactly(fila);

        verify(asistencias).findDeEstudiante("sub-alumno");
        verify(asistencias, never()).findDeEstudiante("otro");
        verifyNoInteractions(catedraClient);
    }

    @Test
    @DisplayName("RN-15: sin un usuario identificable en el token se rechaza con 403")
    void deberiaLanzarAccesoDenegado_cuandoElTokenNoTieneSub() {
        when(usuarioActual.sub()).thenReturn(null);

        assertThrows(AccesoDenegadoException.class, () -> servicio.miAsistencia());

        verifyNoInteractions(asistencias);
    }
}
