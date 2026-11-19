package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaInternaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta las consultas de entregas con sus reglas de acceso.
 *
 * Las llamadas a ms-catedra se hacen antes de consultar la base local. El
 * cálculo de PENDIENTE queda delegado a {@link CalculadorEntregas}.
 */
@Service
public class EntregaConsultaService {

    private final CatedraClient catedraClient;
    private final HitoRepository hitoRepository;
    private final EntregaRepository entregaRepository;
    private final CalculadorEntregas calculadorEntregas;
    private final UsuarioActual usuarioActual;

    public EntregaConsultaService(CatedraClient catedraClient,
                                  HitoRepository hitoRepository,
                                  EntregaRepository entregaRepository,
                                  CalculadorEntregas calculadorEntregas,
                                  UsuarioActual usuarioActual) {
        this.catedraClient = catedraClient;
        this.hitoRepository = hitoRepository;
        this.entregaRepository = entregaRepository;
        this.calculadorEntregas = calculadorEntregas;
        this.usuarioActual = usuarioActual;
    }

    /**
     * D04: entregas de una comisión, opcionalmente filtradas por hito.
     */
    public List<EntregaEstadoResponse> listarPorComision(Long comisionLectivaId, Long hitoId) {
        verificarAccesoAComision(comisionLectivaId);

        List<EquipoDto> equipos = catedraClient.listarEquipos(comisionLectivaId, null);
        if (equipos.isEmpty()) {
            return List.of();
        }

        Long cursoLectivoId = equipos.getFirst().cursoLectivoId();
        List<Hito> hitos = obtenerHitos(cursoLectivoId, hitoId);
        if (hitos.isEmpty()) {
            return List.of();
        }

        List<Long> equipoIds = equipos.stream().map(EquipoDto::id).toList();
        List<Entrega> entregas = buscarEntregas(hitos, equipoIds);
        return calculadorEntregas.calcular(hitos, equipoIds, entregas);
    }

    /**
     * D05: entregas del equipo del alumno autenticado, incluidas pendientes.
     */
    public List<EntregaEstadoResponse> listarMiEquipo() {
        String sub = usuarioActual.sub();
        if (sub == null || sub.isBlank()) {
            throw new AccesoDenegadoException(
                    "No se pudo identificar al alumno autenticado", "RN-15");
        }

        EstudianteDto estudiante = catedraClient.obtenerEstudiantePorSub(sub);
        if (estudiante.equipoId() == null) {
            return List.of();
        }

        List<Hito> hitos = obtenerHitos(estudiante.cursoLectivoId(), null);
        if (hitos.isEmpty()) {
            return List.of();
        }

        List<Entrega> entregas = buscarEntregas(hitos, List.of(estudiante.equipoId()));
        return calculadorEntregas.calcular(hitos, List.of(estudiante.equipoId()), entregas);
    }

    /**
     * I07: mismo cálculo para los equipos y el curso indicados por el llamador.
     */
    public List<EntregaInternaResponse> listarInterno(Long cursoLectivoId, List<Long> equipoIds) {
        if (equipoIds == null || equipoIds.isEmpty()) {
            return List.of();
        }

        List<Hito> hitos = obtenerHitos(cursoLectivoId, null);
        if (hitos.isEmpty()) {
            return List.of();
        }

        List<Long> equiposDistintos = equipoIds.stream().distinct().toList();
        List<Entrega> entregas = buscarEntregas(hitos, equiposDistintos);
        return calculadorEntregas.calcular(hitos, equiposDistintos, entregas).stream()
                .map(EntregaInternaResponse::desde)
                .toList();
    }

    private void verificarAccesoAComision(Long comisionLectivaId) {
        if (usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            return;
        }

        if (!usuarioActual.tieneRol(Roles.DOCENTE)) {
            throw new AccesoDenegadoException(
                    "Solo el jefe o un docente de la comisión pueden consultar las entregas",
                    "RN-01");
        }

        String sub = usuarioActual.sub();
        if (sub == null || sub.isBlank()
                || !catedraClient.docenteAsignado(comisionLectivaId, sub)) {
            throw new AccesoDenegadoException(
                    "El docente no está asignado a la comisión", "RN-01");
        }
    }

    private List<Hito> obtenerHitos(Long cursoLectivoId, Long hitoId) {
        if (hitoId == null) {
            return hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(cursoLectivoId);
        }

        Hito hito = hitoRepository.findByIdAndCursoLectivoId(hitoId, cursoLectivoId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el hito " + hitoId + " en el curso " + cursoLectivoId));
        return List.of(hito);
    }

    private List<Entrega> buscarEntregas(List<Hito> hitos, List<Long> equipoIds) {
        List<Long> hitoIds = hitos.stream().map(Hito::getId).toList();
        return entregaRepository.findByHitoIdInAndEquipoIdIn(hitoIds, equipoIds);
    }
}
