package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Consultas propias y restricciones de las tablas de calendario y asistencia, contra H2. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AsistenciaRepositoryTest {

    private static final Instant T1 = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-09T12:00:00Z");

    @Autowired
    private EventoRepository eventos;
    @Autowired
    private AsistenciaRepository asistencias;

    private Evento eventoViejo;
    private Evento eventoNuevo;

    @BeforeEach
    void preparar() {
        eventoNuevo = eventos.saveAndFlush(evento(10L, T2));
        eventoViejo = eventos.saveAndFlush(evento(10L, T1));
    }

    private static Evento evento(Long comision, Instant fecha) {
        return Evento.builder().comisionLectivaId(comision).tipo(TipoEvento.CLASE).fecha(fecha).build();
    }

    private Asistencia fila(Evento evento, String sub, boolean presente) {
        return asistencias.saveAndFlush(Asistencia.builder().evento(evento).estudianteSub(sub).equipoId(7L)
                .presente(presente).registradaPorSub("sub-doc").fechaRegistro(T2).build());
    }

    @Test
    @DisplayName("RN12: el calendario de una comisión sale del evento más antiguo al más reciente")
    void deberiaListarElCalendarioOrdenadoPorFecha_deLaComision() {
        eventos.saveAndFlush(evento(99L, T1));

        List<Evento> calendario = eventos.findByComisionLectivaIdOrderByFechaAsc(10L);

        assertThat(calendario).extracting(Evento::getId).containsExactly(eventoViejo.getId(), eventoNuevo.getId());
    }

    @Test
    @DisplayName("RN13: no puede haber dos filas del mismo alumno en un evento")
    void deberiaRechazarDosFilasDelMismoAlumno_enUnEvento() {
        fila(eventoViejo, "sub-a", true);

        assertThrows(DataIntegrityViolationException.class, () -> fila(eventoViejo, "sub-a", false));
    }

    @Test
    @DisplayName("RN13: el mismo alumno puede tener una fila en cada evento")
    void deberiaPermitirLaMismaFila_enEventosDistintos() {
        fila(eventoViejo, "sub-a", true);
        fila(eventoNuevo, "sub-a", false);

        assertThat(asistencias.findDeEstudiante("sub-a")).hasSize(2);
    }

    @Test
    @DisplayName("RN13: se encuentra la fila de un alumno en un evento, y no la de otro")
    void deberiaEncontrarLaFila_porEventoYAlumno() {
        fila(eventoViejo, "sub-a", true);

        assertThat(asistencias.findByEventoIdAndEstudianteSub(eventoViejo.getId(), "sub-a")).isPresent();
        assertThat(asistencias.findByEventoIdAndEstudianteSub(eventoViejo.getId(), "sub-b")).isEmpty();
        assertThat(asistencias.findByEventoIdAndEstudianteSub(eventoNuevo.getId(), "sub-a")).isEmpty();
    }

    @Test
    @DisplayName("RN13: se encuentran las filas existentes de un grupo de alumnos en un evento")
    void deberiaEncontrarLasFilasExistentes_deUnGrupoDeAlumnos() {
        fila(eventoViejo, "sub-a", true);
        fila(eventoViejo, "sub-c", true);
        fila(eventoNuevo, "sub-b", true);

        List<Asistencia> encontradas =
                asistencias.findByEventoIdAndEstudianteSubIn(eventoViejo.getId(), List.of("sub-a", "sub-b"));

        assertThat(encontradas).extracting(Asistencia::getEstudianteSub).containsExactly("sub-a");
    }

    @Test
    @DisplayName("RN01: la asistencia de un evento sale ordenada por alumno")
    void deberiaListarLaAsistenciaDeUnEvento_ordenadaPorAlumno() {
        fila(eventoViejo, "sub-b", true);
        fila(eventoViejo, "sub-a", false);

        assertThat(asistencias.findByEventoIdOrderByEstudianteSubAsc(eventoViejo.getId()))
                .extracting(Asistencia::getEstudianteSub).containsExactly("sub-a", "sub-b");
    }

    @Test
    @DisplayName("RN15: la asistencia propia trae los datos del evento, del más reciente al más antiguo, y solo la del alumno")
    void deberiaDevolverSoloLaAsistenciaPropia_conLosDatosDelEvento() {
        fila(eventoViejo, "sub-a", true);
        fila(eventoNuevo, "sub-a", false);
        fila(eventoNuevo, "sub-b", true);

        List<Asistencia> propia = asistencias.findDeEstudiante("sub-a");

        assertThat(propia).hasSize(2);
        assertThat(propia).extracting(a -> a.getEvento().getFecha()).containsExactly(T2, T1);
        assertThat(propia).extracting(Asistencia::getEstudianteSub).containsOnly("sub-a");
    }

    @Test
    @DisplayName("F13: el dashboard solo toma asistencia del equipo y de los eventos de su comisión")
    void deberiaListarLaAsistenciaDelEquipoEnSuComision() {
        Evento otraComision = eventos.saveAndFlush(evento(99L, T1));
        fila(eventoViejo, "sub-a", true);
        fila(eventoNuevo, "sub-b", false);
        asistencias.saveAndFlush(Asistencia.builder().evento(eventoViejo).estudianteSub("sub-c")
                .equipoId(8L).presente(true).registradaPorSub("sub-doc").fechaRegistro(T1).build());
        asistencias.saveAndFlush(Asistencia.builder().evento(otraComision).estudianteSub("sub-d")
                .equipoId(7L).presente(true).registradaPorSub("sub-doc").fechaRegistro(T1).build());

        List<Asistencia> encontradas = asistencias.findDelEquipoEnComision(7L, 10L);

        assertThat(encontradas).extracting(Asistencia::getEstudianteSub).containsExactly("sub-b", "sub-a");
        assertThat(encontradas.get(0).getEvento().getId()).isEqualTo(eventoNuevo.getId());
    }
}
