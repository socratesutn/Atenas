package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.documentacion.client.ArchivoDrive;
import ar.edu.utn.frc.atenas.documentacion.client.DriveClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.exception.ArchivoNoAccesibleException;
import org.springframework.stereotype.Component;

/**
 * Único lugar donde se aplica la regla informativa de RN30.
 *
 * D06 lo usa para mostrar SI/NO/NO_VERIFICADO y D07 para obtener el Boolean
 * que se congela al corregir. Si Drive no se puede consultar, devuelve null:
 * D06 lo traduce a NO_VERIFICADO y D07 lo guarda como NULL.
 */
@Component
public class CalculadorCambioPostEntrega {

    private final DriveClient driveClient;

    public CalculadorCambioPostEntrega(DriveClient driveClient) {
        this.driveClient = driveClient;
    }

    /**
     * Resultado para una apertura D06.
     */
    public CambioPostEntrega paraApertura(Entrega entrega, ArchivoVerificado archivo) {
        if (entrega.getEstado() == EstadoEntrega.CORREGIDA) {
            return valorCongelado(entrega);
        }

        Boolean cambio = calcularConDrive(entrega, archivo);
        if (cambio == null) {
            return CambioPostEntrega.NO_VERIFICADO;
        }
        return cambio ? CambioPostEntrega.SI : CambioPostEntrega.NO;
    }

    /**
     * Comparación con Drive para ENTREGADA.
     *
     * @return true/false si se pudo verificar; null si falta información o
     *         Drive falla.
     */
    public Boolean calcularConDrive(Entrega entrega, ArchivoVerificado archivo) {
        if (archivo == null
                || archivo.getDriveFileId() == null
                || archivo.getDriveFileId().isBlank()
                || archivo.getModifiedTimeEntrega() == null) {
            return null;
        }

        try {
            ArchivoDrive actual = driveClient.obtenerArchivo(archivo.getDriveFileId());
            if (actual == null || actual.modifiedTime() == null) {
                return null;
            }

            return actual.modifiedTime().isAfter(archivo.getModifiedTimeEntrega())
                    && actual.modifiedTime().isAfter(entrega.getHito().getFechaLimite());
        } catch (NoEncontradoException | ServicioNoDisponibleException
                 | ArchivoNoAccesibleException excepcion) {
            return null;
        }
    }

    private CambioPostEntrega valorCongelado(Entrega entrega) {
        if (entrega.getModificadoPostEntrega() == null) {
            return CambioPostEntrega.NO_VERIFICADO;
        }
        return entrega.getModificadoPostEntrega()
                ? CambioPostEntrega.SI
                : CambioPostEntrega.NO;
    }
}
