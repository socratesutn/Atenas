package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.mapper.EntregaAperturaMapper;
import ar.edu.utn.frc.atenas.documentacion.repository.ArchivoVerificadoRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import org.springframework.stereotype.Service;

/**
 * Orquesta D06: abrir una entrega, validar pertenencia del docente y calcular
 * el aviso de cambios posteriores.
 *
 * La comparación de RN30 se delega a {@link CalculadorCambioPostEntrega}.
 */
@Service
public class EntregaAperturaService {

    private final CatedraClient catedraClient;
    private final EntregaRepository entregaRepository;
    private final ArchivoVerificadoRepository archivoVerificadoRepository;
    private final CalculadorCambioPostEntrega calculadorCambio;
    private final UsuarioActual usuarioActual;
    private final ZonaHoraria zonaHoraria;

    public EntregaAperturaService(CatedraClient catedraClient,
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

    public EntregaAperturaResponse abrir(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la entrega " + entregaId));

        EquipoDto equipo = catedraClient.obtenerEquipo(entrega.getEquipoId());
        validarDocenteAsignado(equipo.comisionLectivaId());

        ArchivoVerificado archivo = archivoVerificadoRepository
                .findByEntregaId(entregaId)
                .orElse(null);

        CambioPostEntrega cambio = calculadorCambio.paraApertura(entrega, archivo);
        return EntregaAperturaMapper.aResponse(entrega, archivo, cambio, zonaHoraria);
    }

    private void validarDocenteAsignado(Long comisionLectivaId) {
        String sub = usuarioActual.sub();
        if (sub == null || sub.isBlank()
                || !catedraClient.docenteAsignado(comisionLectivaId, sub)) {
            throw new AccesoDenegadoException(
                    "El docente no está asignado a la comisión", "RN-01");
        }
    }
}
