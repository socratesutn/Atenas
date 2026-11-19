package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.ArchivoDrive;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.client.DriveClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.repository.ArchivoVerificadoRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaCorreccionServiceTest {

    @Mock
    private CatedraClient catedraClient;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private ArchivoVerificadoRepository archivoVerificadoRepository;

    @Mock
    private DriveClient driveClient;

    @Mock
    private UsuarioActual usuarioActual;

    private EntregaCorreccionService service;
    private Hito hito;
    private Entrega entrega;

    @BeforeEach
    void preparar() {
        service = new EntregaCorreccionService(
                catedraClient,
                entregaRepository,
                archivoVerificadoRepository,
                new CalculadorCambioPostEntrega(driveClient),
                usuarioActual,
                new ZonaHoraria("America/Argentina/Cordoba"));
        hito = new Hito(3L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse("2026-03-01T03:00:00Z"),
                Instant.parse("2026-03-16T02:59:59Z"));
        entrega = new Entrega(10L, hito, 7L, EstadoEntrega.ENTREGADA, false, null);
    }

    @ParameterizedTest(name = "nota {0} responde 422")
    @ValueSource(ints = {0, 11})
    @DisplayName("RN-24: una nota fuera de 1 a 10 responde 422")
    void deberiaRechazarNotaFueraDeRango(int nota) {
        assertThrows(NoProcesableException.class,
                () -> service.corregir(10L, nota));
        verify(entregaRepository, never()).save(any());
    }

    @ParameterizedTest(name = "nota {0} se acepta")
    @ValueSource(ints = {1, 10})
    @DisplayName("RN-24: acepta los extremos 1 y 10")
    void deberiaAceptarLosExtremosDeNota(int nota) {
        prepararCorreccionOk();
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

        EntregaAperturaResponse respuesta = service.corregir(10L, nota);

        assertThat(respuesta.estado()).isEqualTo("CORREGIDA");
        assertThat(respuesta.nota()).isEqualTo(nota);
        assertThat(entrega.getFechaCorreccion()).isNotNull();
        assertThat(entrega.getCorregidaPorSub()).isEqualTo("sub-doc");
    }

    @Test
    @DisplayName("RN-24: una entrega ya CORREGIDA responde 409")
    void deberiaRechazarEntregaYaCorregida() {
        entrega.setEstado(EstadoEntrega.CORREGIDA);
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));

        assertThrows(ConflictoException.class, () -> service.corregir(10L, 8));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-24: una entrega que no está ENTREGADA responde 409")
    void deberiaRechazarEntregaNoEntregada() {
        entrega.setEstado(null);
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));

        assertThrows(ConflictoException.class, () -> service.corregir(10L, 8));
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión responde 403")
    void deberiaRechazarDocenteAjeno() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipo());
        when(usuarioActual.sub()).thenReturn("sub-otro");
        when(catedraClient.docenteAsignado(10L, "sub-otro")).thenReturn(false);

        assertThrows(AccesoDenegadoException.class, () -> service.corregir(10L, 8));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-30: si Drive falla, la corrección se guarda con congelado NULL")
    void deberiaGuardarConNull_cuandoDriveFalla() {
        prepararCorreccionOk();
        when(driveClient.obtenerArchivo("file-1"))
                .thenThrow(new ServicioNoDisponibleException("caído"));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

        EntregaAperturaResponse respuesta = service.corregir(10L, 8);

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.CORREGIDA);
        assertThat(entrega.getNota()).isEqualTo(8);
        assertThat(entrega.getModificadoPostEntrega()).isNull();
        assertThat(respuesta.modificadoPostEntrega()).isEqualTo(CambioPostEntrega.NO_VERIFICADO);
    }

    @Test
    @DisplayName("D07: si ms-catedra falla responde 503 y no guarda")
    void deberiaResponder503_cuandoFallaCatedra() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));
        when(catedraClient.obtenerEquipo(7L))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.corregir(10L, 8));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-30: con Drive disponible guarda el valor calculado")
    void deberiaGuardarElCambioCalculado_cuandoDriveResponde() {
        prepararCorreccionOk();
        when(driveClient.obtenerArchivo("file-1")).thenReturn(
                new ArchivoDrive("file-1", "informe.pdf", "application/pdf",
                        Instant.parse("2026-04-01T12:00:00Z")));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

        EntregaAperturaResponse respuesta = service.corregir(10L, 8);

        assertThat(entrega.getModificadoPostEntrega()).isTrue();
        assertThat(respuesta.modificadoPostEntrega()).isEqualTo(CambioPostEntrega.SI);
    }

    private void prepararCorreccionOk() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipo());
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(true);
        when(archivoVerificadoRepository.findByEntregaId(10L))
                .thenReturn(Optional.of(archivo()));
    }

    private ArchivoVerificado archivo() {
        return new ArchivoVerificado(20L, entrega,
                "https://drive.google.com/file/d/file-1", "file-1",
                "informe.pdf", "application/pdf",
                Instant.parse("2026-03-10T12:00:00Z"));
    }

    private EquipoDto equipo() {
        return new EquipoDto(7L, "Equipo 1", 10L, 2L, "ASEGURADO", true, "sub-rep", List.of());
    }
}
