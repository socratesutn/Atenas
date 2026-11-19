package ar.edu.utn.frc.atenas.documentacion.client;

/**
 * Adaptador de salida hacia Google Drive.
 *
 * D06 solo necesita leer los metadatos actuales de un archivo para comparar
 * su modifiedTime. Las fallas del proveedor se informan mediante excepciones
 * de commons y el servicio decide degradar a NO_VERIFICADO.
 */
public interface DriveClient {

    /** Extrae el fileId de los formatos habituales de enlaces de Drive/Docs. */
    String extraerFileId(String enlace);

    ArchivoDrive obtenerArchivo(String driveFileId);
}
