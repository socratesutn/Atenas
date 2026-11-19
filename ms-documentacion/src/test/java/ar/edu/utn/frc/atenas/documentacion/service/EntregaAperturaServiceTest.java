package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaAperturaServiceTest {

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

    private EntregaAperturaService service;
    private Hito hito;

    @BeforeEach
    void preparar() {
        service = new EntregaAperturaService(
                catedraClient,
                entregaRepository,
                archivoVerificadoRepository,
                new CalculadorCambioPostEntrega(driveClient),
                usuarioActual,
                new ZonaHoraria("America/Argentina/Cordoba"));
        hito = new Hito(3L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse("2026-03-01T03:00:00Z"),
                Instant.parse("2026-03-16T02:59:59Z"));
    }

    @Test
    @DisplayName("RN-30: devuelve SI cuando Drive cambió después de la entrega y del límite")
    void deberiaDevolverSi_cuandoElArchivoCambioDespuesDeEntregaYLimite() {
        prepararEntregaEntregada();
        when(driveClient.obtenerArchivo("file-1")).thenReturn(
                archivoDrive(Instant.parse("2026-04-01T12:00:00Z")));

        EntregaAperturaResponse respuesta = service.abrir(10L);

        assertThat(respuesta.modificadoPostEntrega()).isEqualTo(CambioPostEntrega.SI);
        assertThat(respuesta.hito().id()).isEqualTo(3L);
        assertThat(respuesta.hito().nombre()).isEqualTo("Sprint 1");
        assertThat(respuesta.enlace()).isEqualTo("https://drive.google.com/file/d/file-1");
    }

    @Test
    @DisplayName("RN-30: devuelve NO cuando cambió después de la entrega pero no del límite")
    void deberiaDevolverNo_cuandoElCambioNoSuperaElLimite() {
        prepararEntregaEntregada();
        when(driveClient.obtenerArchivo("file-1")).thenReturn(
                archivoDrive(Instant.parse("2026-03-12T12:00:00Z")));

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.NO);
    }

    @Test
    @DisplayName("RN-30: devuelve NO cuando el archivo no cambió después de la entrega")
    void deberiaDevolverNo_cuandoElArchivoNoCambioDespuesDeLaEntrega() {
        prepararEntregaEntregada();
        when(driveClient.obtenerArchivo("file-1")).thenReturn(
                archivoDrive(Instant.parse("2026-03-09T12:00:00Z")));

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.NO);
    }

    @Test
    @DisplayName("RN-30: si Drive falla devuelve NO_VERIFICADO y no rompe la apertura")
    void deberiaDevolverNoVerificado_cuandoDriveFalla() {
        prepararEntregaEntregada();
        when(driveClient.obtenerArchivo("file-1"))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.NO_VERIFICADO);
    }

    @Test
    @DisplayName("RN-30: CORREGIDA usa el valor congelado y no llama a Drive")
    void deberiaUsarElValorCongelado_cuandoLaEntregaEstaCorregida() {
        prepararEntregaCorregida(true);

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.SI);
        verifyNoInteractions(driveClient);
    }

    @Test
    @DisplayName("RN-30: CORREGIDA con valor congelado false devuelve NO")
    void deberiaDevolverNo_cuandoLaCorregidaTieneFalseCongelado() {
        prepararEntregaCorregida(false);

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.NO);
        verifyNoInteractions(driveClient);
    }

    @Test
    @DisplayName("RN-30: CORREGIDA sin valor congelado devuelve NO_VERIFICADO")
    void deberiaDevolverNoVerificado_cuandoLaCorregidaNoTieneValorCongelado() {
        prepararEntregaCorregida(null);

        assertThat(service.abrir(10L).modificadoPostEntrega())
                .isEqualTo(CambioPostEntrega.NO_VERIFICADO);
        verifyNoInteractions(driveClient);
    }

    @Test
    @DisplayName("D06: una entrega inexistente devuelve 404")
    void deberiaLanzarNoEncontrado_cuandoLaEntregaNoExiste() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> service.abrir(10L));
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión recibe 403")
    void deberiaLanzarAccesoDenegado_cuandoElDocenteNoEstaAsignado() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entregaEntregada()));
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipo());
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(false);

        assertThrows(AccesoDenegadoException.class, () -> service.abrir(10L));
        verify(archivoVerificadoRepository, never()).findByEntregaId(10L);
    }

    @Test
    @DisplayName("D06: si ms-catedra falla responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaCatedra() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entregaEntregada()));
        when(catedraClient.obtenerEquipo(7L))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> service.abrir(10L));
    }

    private void prepararEntregaEntregada() {
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entregaEntregada()));
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipo());
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(true);
        when(archivoVerificadoRepository.findByEntregaId(10L))
                .thenReturn(Optional.of(archivo()));
    }

    private void prepararEntregaCorregida(Boolean modificado) {
        Entrega entrega = entregaEntregada();
        entrega.setEstado(EstadoEntrega.CORREGIDA);
        entrega.setModificadoPostEntrega(modificado);
        when(entregaRepository.findById(10L)).thenReturn(Optional.of(entrega));
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipo());
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(true);
        when(archivoVerificadoRepository.findByEntregaId(10L))
                .thenReturn(Optional.of(archivo()));
    }

    private Entrega entregaEntregada() {
        return new Entrega(10L, hito, 7L, EstadoEntrega.ENTREGADA, false, null);
    }

    private ArchivoVerificado archivo() {
        return new ArchivoVerificado(20L, entregaEntregada(),
                "https://drive.google.com/file/d/file-1", "file-1",
                "informe.pdf", "application/pdf",
                Instant.parse("2026-03-10T12:00:00Z"));
    }

    private ArchivoDrive archivoDrive(Instant modifiedTime) {
        return new ArchivoDrive("file-1", "informe.pdf", "application/pdf", modifiedTime);
    }

    private EquipoDto equipo() {
        return new EquipoDto(7L, "Equipo 1", 10L, 2L, "ASEGURADO", true, "sub-rep", List.of());
    }
}
