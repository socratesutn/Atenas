package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CrearHitoRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.HitoResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.CursoLectivoDto;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Reglas de hitos: D01, D02 e I06.
 *
 * El curso y sus fechas se consultan a ms-catedra antes de persistir. La
 * validación de fechas se hace contra el período del curso y la zona horaria
 * configurada.
 */
@Service
public class HitoService {

    private final HitoRepository hitoRepository;
    private final CatedraClient catedraClient;
    private final UsuarioActual usuarioActual;
    private final ZonaHoraria zonaHoraria;

    public HitoService(HitoRepository hitoRepository,
                       CatedraClient catedraClient,
                       UsuarioActual usuarioActual,
                       ZonaHoraria zonaHoraria) {
        this.hitoRepository = hitoRepository;
        this.catedraClient = catedraClient;
        this.usuarioActual = usuarioActual;
        this.zonaHoraria = zonaHoraria;
    }

    public HitoResponse crear(CrearHitoRequest request) {
        if (!usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            throw new AccesoDenegadoException("Solo el jefe puede definir hitos", "RN-17");
        }

        CursoLectivoDto curso = catedraClient.obtenerCurso(request.cursoLectivoId());
        validarFechas(request, curso);

        Hito hito = new Hito();
        hito.setCursoLectivoId(request.cursoLectivoId());
        hito.setNombre(request.nombre());
        hito.setTipo(request.tipo());
        hito.setOrden(request.orden());
        hito.setFechaInicio(request.fechaInicio());
        hito.setFechaLimite(request.fechaLimite());
        return aResponse(hitoRepository.save(hito));
    }

    public List<HitoResponse> listar(Long cursoLectivoId) {
        if (cursoLectivoId == null) {
            throw new SolicitudInvalidaException("cursoLectivoId es obligatorio");
        }
        return hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(cursoLectivoId)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    public List<HitoResponse> internos(Long cursoLectivoId, String tipo) {
        if (cursoLectivoId == null) {
            throw new SolicitudInvalidaException("cursoLectivoId es obligatorio");
        }

        TipoHito filtro = parsearTipo(tipo);
        return hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(cursoLectivoId)
                .stream()
                .filter(hito -> filtro == null || hito.getTipo() == filtro)
                .map(this::aResponse)
                .toList();
    }

    private void validarFechas(CrearHitoRequest request, CursoLectivoDto curso) {
        Instant inicioCurso = curso.fechaInicio()
                .atStartOfDay(zonaHoraria.zona()).toInstant();
        Instant finCurso = curso.fechaFin()
                .plusDays(1)
                .atStartOfDay(zonaHoraria.zona())
                .minusNanos(1)
                .toInstant();

        if (!request.fechaInicio().isBefore(request.fechaLimite())) {
            throw new SolicitudInvalidaException(
                    "fechaInicio debe ser anterior a fechaLimite");
        }
        if (request.fechaInicio().isBefore(inicioCurso)
                || request.fechaLimite().isAfter(finCurso)) {
            throw new SolicitudInvalidaException(
                    "Las fechas deben estar dentro del período del curso");
        }
    }

    private TipoHito parsearTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return TipoHito.valueOf(tipo);
        } catch (IllegalArgumentException excepcion) {
            throw new SolicitudInvalidaException("Tipo de hito inválido: " + tipo);
        }
    }

    private HitoResponse aResponse(Hito hito) {
        return new HitoResponse(
                hito.getId(),
                hito.getCursoLectivoId(),
                hito.getNombre(),
                hito.getTipo(),
                hito.getOrden(),
                zonaHoraria.aOffsetDateTime(hito.getFechaInicio()),
                zonaHoraria.aOffsetDateTime(hito.getFechaLimite()));
    }
}
