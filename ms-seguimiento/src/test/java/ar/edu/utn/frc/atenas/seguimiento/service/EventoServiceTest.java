package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    private static final Long COMISION = 10L;
    private static final Instant FECHA = Instant.parse("2026-11-03T12:00:00Z");

    @Mock
    private EventoRepository eventos;
    @Mock
    private AccesoComision acceso;

    private EventoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EventoService(eventos, acceso);
    }

    @Test
    @DisplayName("RN-12: un docente asignado crea una clase y se guarda en su comisión")
    void deberiaCrearElEvento_cuandoElDocenteEstaAsignado() {
        when(eventos.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        Evento creado = servicio.crear(COMISION, TipoEvento.CLASE, FECHA, "  Clase de repaso ");

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventos).save(captor.capture());
        assertThat(captor.getValue().getComisionLectivaId()).isEqualTo(COMISION);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoEvento.CLASE);
        assertThat(captor.getValue().getFecha()).isEqualTo(FECHA);
        assertThat(creado.getDescripcion()).isEqualTo("Clase de repaso");
        verify(acceso).exigirDocenteAsignado(COMISION, "RN-12");
    }

    @Test
    @DisplayName("RN-12: una descripción en blanco se guarda como nula")
    void deberiaGuardarDescripcionNula_cuandoLlegaEnBlanco() {
        when(eventos.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(servicio.crear(COMISION, TipoEvento.PRESENTACION, FECHA, "   ").getDescripcion()).isNull();
        assertThat(servicio.crear(COMISION, TipoEvento.PRESENTACION, FECHA, null).getDescripcion()).isNull();
    }

    @Test
    @DisplayName("RN-12: un docente no asignado recibe 403 y no se crea nada")
    void deberiaPropagarAccesoDenegado_cuandoElDocenteNoEstaAsignado() {
        doThrow(new AccesoDenegadoException("No asignado", "RN-12")).when(acceso).exigirDocenteAsignado(COMISION, "RN-12");

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.crear(COMISION, TipoEvento.CLASE, FECHA, null));

        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("RN-12: si ms-catedra no responde, 503 y no se crea nada")
    void deberiaPropagarServicioNoDisponible_cuandoMsCatedraNoResponde() {
        doThrow(new ServicioNoDisponibleException("ms-catedra caído")).when(acceso).exigirDocenteAsignado(COMISION, "RN-12");

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.crear(COMISION, TipoEvento.CLASE, FECHA, null));

        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("RN-12: una comisión inexistente (404 de ms-catedra) se propaga y no se crea nada")
    void deberiaPropagarNoEncontrado_cuandoLaComisionNoExiste() {
        doThrow(new NoEncontradoException("No existe la comisión")).when(acceso).exigirDocenteAsignado(COMISION, "RN-12");

        assertThrows(NoEncontradoException.class,
                () -> servicio.crear(COMISION, TipoEvento.CLASE, FECHA, null));

        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("RN-01: el calendario se devuelve ordenado después de verificar el acceso")
    void deberiaListarLosEventos_cuandoTieneAcceso() {
        Evento evento = Evento.builder().id(1L).comisionLectivaId(COMISION).tipo(TipoEvento.CLASE).fecha(FECHA).build();
        when(eventos.findByComisionLectivaIdOrderByFechaAsc(COMISION)).thenReturn(List.of(evento));

        assertThat(servicio.listar(COMISION)).containsExactly(evento);

        verify(acceso).exigirJefeDocenteOAlumno(COMISION);
    }

    @Test
    @DisplayName("RN-01: sin acceso a la comisión no se consulta el calendario")
    void deberiaPropagarAccesoDenegado_alListarSinAcceso() {
        doThrow(new AccesoDenegadoException("Sin acceso", "RN-01")).when(acceso).exigirJefeDocenteOAlumno(COMISION);

        assertThrows(AccesoDenegadoException.class, () -> servicio.listar(COMISION));

        verifyNoInteractions(eventos);
    }
}
