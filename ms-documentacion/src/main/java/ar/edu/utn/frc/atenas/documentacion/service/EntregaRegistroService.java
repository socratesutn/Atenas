package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.ArchivoDrive;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.client.DriveClient;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.RegistrarEntregaRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.exception.ArchivoNoAccesibleException;
import ar.edu.utn.frc.atenas.documentacion.repository.ArchivoVerificadoRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * D03: registra una entrega verificada en Drive.
 *
 * Las consultas a ms-catedra y Drive se hacen antes de abrir la transacción;
 * Entrega y ArchivoVerificado se guardan juntas.
 */
@Service
public class EntregaRegistroService {

    private final HitoRepository hitoRepository;
    private final EntregaRepository entregaRepository;
    private final ArchivoVerificadoRepository archivoRepository;
    private final CatedraClient catedraClient;
    private final DriveClient driveClient;
    private final UsuarioActual usuarioActual;
    private final ZonaHoraria zonaHoraria;
    private final TransactionTemplate transactionTemplate;

    public EntregaRegistroService(HitoRepository hitoRepository,
                                  EntregaRepository entregaRepository,
                                  ArchivoVerificadoRepository archivoRepository,
                                  CatedraClient catedraClient,
                                  DriveClient driveClient,
                                  UsuarioActual usuarioActual,
                                  ZonaHoraria zonaHoraria,
                                  PlatformTransactionManager txManager) {
        this.hitoRepository = hitoRepository;
        this.entregaRepository = entregaRepository;
        this.archivoRepository = archivoRepository;
        this.catedraClient = catedraClient;
        this.driveClient = driveClient;
        this.usuarioActual = usuarioActual;
        this.zonaHoraria = zonaHoraria;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    public EntregaResponse registrar(Long hitoId, RegistrarEntregaRequest request) {
        if (!usuarioActual.tieneRol(Roles.ALUMNO)) {
            throw new AccesoDenegadoException("Solo un alumno puede registrar una entrega");
        }

        String sub = usuarioActual.sub();
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new NoEncontradoException("Hito no encontrado"));

        EstudianteDto estudiante = catedraClient.obtenerEstudiantePorSub(sub);
        if (estudiante.equipoId() == null) {
            throw new AccesoDenegadoException("El alumno no tiene equipo");
        }

        EquipoDto equipo = catedraClient.obtenerEquipo(estudiante.equipoId());
        if (!sub.equals(equipo.representanteSub())) {
            throw new AccesoDenegadoException("Solo el representante puede registrar la entrega");
        }
        if (!hito.getCursoLectivoId().equals(equipo.cursoLectivoId())) {
            throw new AccesoDenegadoException(
                    "El hito no pertenece al curso del equipo");
        }
        if (equipo.asegurado() == null || !equipo.asegurado()) {
            throw new ConflictoException("El equipo no está asegurado");
        }

        if (entregaRepository.findByHitoIdAndEquipoId(hitoId, equipo.id()).isPresent()) {
            throw new ConflictoException("Ya existe una entrega para este hito y equipo");
        }

        String fileId = driveClient.extraerFileId(request.enlace());
        ArchivoDrive archivo;
        try {
            archivo = driveClient.obtenerArchivo(fileId);
        } catch (ArchivoNoAccesibleException excepcion) {
            throw new NoProcesableException("El archivo de Drive no es accesible");
        }

        if (!"application/pdf".equals(archivo.mimeType())
                && !"application/vnd.google-apps.document".equals(archivo.mimeType())) {
            throw new NoProcesableException("El archivo debe ser PDF o Google Docs");
        }

        Instant ahora = Instant.now();
        boolean tardia = ahora.isAfter(hito.getFechaLimite());

        Resultado resultado;
        try {
            resultado = transactionTemplate.execute(status -> guardar(
                    hito, equipo.id(), sub, request.enlace(), archivo, ahora, tardia));
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("Ya existe una entrega para este hito y equipo");
        }

        return new EntregaResponse(
                resultado.entrega().getId(),
                resultado.entrega().getHito().getId(),
                resultado.entrega().getEquipoId(),
                resultado.entrega().getEstado(),
                resultado.entrega().getTardia(),
                zonaHoraria.aOffsetDateTime(resultado.entrega().getFechaEntrega()),
                resultado.archivo().getEnlace(),
                resultado.archivo().getId());
    }

    private Resultado guardar(Hito hito, Long equipoId, String sub, String enlace,
                              ArchivoDrive archivo, Instant ahora, boolean tardia) {
        Entrega entrega = new Entrega();
        entrega.setHito(hito);
        entrega.setEquipoId(equipoId);
        entrega.setEstado(EstadoEntrega.ENTREGADA);
        entrega.setTardia(tardia);
        entrega.setFechaEntrega(ahora);
        entrega.setRegistradaPorSub(sub);
        Entrega guardada = entregaRepository.saveAndFlush(entrega);

        ArchivoVerificado archivoVerificado = new ArchivoVerificado();
        archivoVerificado.setEntrega(guardada);
        archivoVerificado.setEnlace(enlace);
        archivoVerificado.setDriveFileId(archivo.id());
        archivoVerificado.setNombre(archivo.nombre());
        archivoVerificado.setMimeType(archivo.mimeType());
        archivoVerificado.setModifiedTimeEntrega(archivo.modifiedTime());
        ArchivoVerificado archivoGuardado = archivoRepository.save(archivoVerificado);

        return new Resultado(guardada, archivoGuardado);
    }

    private record Resultado(Entrega entrega, ArchivoVerificado archivo) {
    }
}
