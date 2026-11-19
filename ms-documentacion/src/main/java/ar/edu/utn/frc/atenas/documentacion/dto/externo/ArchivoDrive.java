package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import java.time.Instant;

/**
 * Representacion propia de un archivo verificado en Google Drive.
 */
public record ArchivoDrive(String fileId, String nombre, String mimeType, Instant modifiedTime) { }