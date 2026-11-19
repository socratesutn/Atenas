package ar.edu.utn.frc.atenas.documentacion.client;

import java.time.Instant;

/** Metadatos mínimos obtenidos de Google Drive para verificar un archivo. */
public record ArchivoDrive(
        String id,
        String nombre,
        String mimeType,
        Instant modifiedTime) {
}
