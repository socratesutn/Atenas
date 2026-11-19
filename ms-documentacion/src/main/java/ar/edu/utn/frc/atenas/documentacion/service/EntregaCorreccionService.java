package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.mapper.EntregaAperturaMapper;
import ar.edu.utn.frc.atenas.documentacion.repository.ArchivoVerificadoRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Orquesta D07: corregir una entrega ENTREGADA.
 *
 * Las llamadas a ms-catedra y Drive se hacen antes de guardar. La comparación
 * de RN30 se reutiliza desde {@link CalculadorCambioPostEntrega}; si Drive
 * falla, el valor congelado queda null y la corrección no se bloquea.
 */
@Service
public class EntregaCorreccionService {

    private final CatedraClient catedraClient;
    private final EntregaRepository entregaRepository;
    private final ArchivoVerificadoRepository archivoVerificadoRepository;
    private final CalculadorCambioPostEntrega calculadorCambio;
    private final UsuarioActual usuarioActual;
    private final ZonaHoraria zonaHoraria;

    public EntregaCorreccionService(CatedraClient catedraClient,
                                    EntregaRepository entregaRepository,
                                    ArchivoVerificadoRepository archivoVerificadoRepository,
                                    CalculadorCambioPostEntrega calculadorCambio,
                                    UsuarioActual usuarioActual,
                                    ZonaHoraria zonaHoraria) {
        this.catedraClient = catedraClient;
        this.entregaRepository = entregaRepository;
        this.archivoVerificadoRepository = archivoVerificadoRepository;
        this.calculadorCambio = calculadorCambio;
        this.usuarioActual = usuarioActual;
        this.zonaHoraria = zonaHoraria;
    }

    public EntregaAperturaResponse corregir(Long entregaId, Integer nota) {
        validarNota(nota);

        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la entrega " + entregaId));

        if (entrega.getEstado() != EstadoEntrega.ENTREGADA) {
            throw new ConflictoException(
                    "La entrega ya está corregida o no está en estado ENTREGADA", "RN-24");
        }

        EquipoDto equipo = catedraClient.obtenerEquipo(entrega.getEquipoId());
        String sub = usuarioActual.sub();
        validarDocenteAsignado(equipo.comisionLectivaId(), sub);

        ArchivoVerificado archivo = archivoVerificadoRepository
                .findByEntregaId(entregaId)
                .orElse(null);

        Boolean modificadoPostEntrega = calculadorCambio.calcularConDrive(entrega, archivo);

        entrega.setNota(nota);
        entrega.setFechaCorreccion(Instant.now());
        entrega.setCorregidaPorSub(sub);
        entrega.setEstado(EstadoEntrega.CORREGIDA);
        entrega.setModificadoPostEntrega(modificadoPostEntrega);
        Entrega guardada = entregaRepository.save(entrega);

        CambioPostEntrega cambio = calculadorCambio.paraApertura(guardada, archivo);
        return EntregaAperturaMapper.aResponse(guardada, archivo, cambio, zonaHoraria);
    }

    private void validarNota(Integer nota) {
        if (nota == null || nota < 1 || nota > 10) {
            throw new NoProcesableException(
                    "La nota debe ser un número entero entre 1 y 10", "RN-24");
        }
    }

    private void validarDocenteAsignado(Long comisionLectivaId, String sub) {
        if (sub == null || sub.isBlank()
                || !catedraClient.docenteAsignado(comisionLectivaId, sub)) {
            throw new AccesoDenegadoException(
                    "El docente no está asignado a la comisión", "RN-01");
        }
    }
}
