package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.ArchivoDrive;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.client.DriveClient;
import ar.edu.utn.frc.atenas.documentacion.dto.RegistrarEntregaRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.exception.ArchivoNoAccesibleException;
import ar.edu.utn.frc.atenas.documentacion.repository.ArchivoVerificadoRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntregaRegistroServiceTest {

    private static final String ENLACE = "https://drive.google.com/file/d/F1/view";

    @Mock
    private HitoRepository hitoRepository;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private ArchivoVerificadoRepository archivoRepository;

    @Mock
    private CatedraClient catedraClient;

    @Mock
    private DriveClient driveClient;

    @Mock
    private UsuarioActual usuarioActual;

    @Mock
    private PlatformTransactionManager txManager;

    private EntregaRegistroService service;

    @BeforeEach
    void preparar() {
        when(txManager.getTransaction(any())).thenReturn(
                new DefaultTransactionStatus(null, true, false, false, false, null));
        service = new EntregaRegistroService(hitoRepository, entregaRepository, archivoRepository,
                catedraClient, driveClient, usuarioActual,
                new ZonaHoraria("America/Argentina/Cordoba"), txManager);
        when(usuarioActual.tieneRol(Roles.ALUMNO)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-alumno");
    }

    @Test
    @DisplayName("D03: registra entrega y archivo en una transacción")
    void deberiaRegistrarEntrega_cuandoTodoEsValido() {
        prepararCaminoFeliz();

        var response = service.registrar(3L, new RegistrarEntregaRequest(ENLACE));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.estado().name()).isEqualTo("ENTREGADA");
        assertThat(response.enlace()).isEqualTo(ENLACE);
        assertThat(response.archivoId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("D03: un no representante recibe 403")
    void deberiaRechazar_cuandoNoEsRepresentante() {
        prepararHitoYEstudiante();
        when(catedraClient.obtenerEquipo(7L)).thenReturn(
                new EquipoDto(7L, "Equipo", 10L, 2L, "ASEGURADO", true, "otro", List.of()));

        assertThrows(AccesoDenegadoException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: un equipo no asegurado responde 409")
    void deberiaRechazar_cuandoElEquipoNoEstaAsegurado() {
        prepararHitoYEstudiante();
        when(catedraClient.obtenerEquipo(7L)).thenReturn(
                new EquipoDto(7L, "Equipo", 10L, 2L, "ABIERTO", false, "sub-alumno", List.of()));

        assertThrows(ConflictoException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: una entrega duplicada responde 409")
    void deberiaRechazar_cuandoYaExisteLaEntrega() {
        prepararCaminoHastaDuplicado();
        when(entregaRepository.findByHitoIdAndEquipoId(3L, 7L))
                .thenReturn(Optional.of(new Entrega()));

        assertThrows(ConflictoException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: archivo privado o inexistente responde 422")
    void deberiaRechazar_cuandoElArchivoNoEsAccesible() {
        prepararCaminoHastaDrive();
        when(driveClient.obtenerArchivo("F1"))
                .thenThrow(new ArchivoNoAccesibleException("privado"));

        assertThrows(NoProcesableException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: un archivo que no es PDF/Docs responde 422")
    void deberiaRechazar_cuandoElTipoNoEsPermitido() {
        prepararCaminoHastaDrive();
        when(driveClient.obtenerArchivo("F1")).thenReturn(
                new ArchivoDrive("F1", "imagen.png", "image/png", Instant.now()));

        assertThrows(NoProcesableException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: Drive caído responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaDrive() {
        prepararCaminoHastaDrive();
        when(driveClient.obtenerArchivo("F1"))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: ms-catedra caído responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaCatedra() {
        when(hitoRepository.findById(3L)).thenReturn(Optional.of(hito()));
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    @Test
    @DisplayName("D03: un hito inexistente responde 404")
    void deberiaLanzarNoEncontrado_cuandoElHitoNoExiste() {
        when(hitoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> service.registrar(3L, new RegistrarEntregaRequest(ENLACE)));
    }

    private void prepararCaminoFeliz() {
        prepararCaminoHastaDrive();
        when(driveClient.obtenerArchivo("F1")).thenReturn(
                new ArchivoDrive("F1", "informe.pdf", "application/pdf", Instant.now()));
        when(entregaRepository.saveAndFlush(any(Entrega.class))).thenAnswer(inv -> {
            Entrega entrega = inv.getArgument(0);
            entrega.setId(99L);
            return entrega;
        });
        when(archivoRepository.save(any(ArchivoVerificado.class))).thenAnswer(inv -> {
            ArchivoVerificado archivo = inv.getArgument(0);
            archivo.setId(50L);
            return archivo;
        });
    }

    private void prepararCaminoHastaDuplicado() {
        prepararHitoYEstudiante();
        when(catedraClient.obtenerEquipo(7L)).thenReturn(equipoAsegurado());
    }

    private void prepararCaminoHastaDrive() {
        prepararCaminoHastaDuplicado();
        when(entregaRepository.findByHitoIdAndEquipoId(3L, 7L))
                .thenReturn(Optional.empty());
        when(driveClient.extraerFileId(ENLACE)).thenReturn("F1");
    }

    private void prepararHitoYEstudiante() {
        when(hitoRepository.findById(3L)).thenReturn(Optional.of(hito()));
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenReturn(new EstudianteDto(5L, "sub-alumno", true, 10L, 2L, 7L));
    }

    private EquipoDto equipoAsegurado() {
        return new EquipoDto(7L, "Equipo", 10L, 2L, "ASEGURADO", true,
                "sub-alumno", List.of());
    }

    private Hito hito() {
        return new Hito(3L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T00:00:00Z"));
    }
}
