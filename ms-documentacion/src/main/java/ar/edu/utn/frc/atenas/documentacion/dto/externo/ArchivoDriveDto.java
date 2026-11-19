package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Respuesta de Google Drive para la consulta de metadatos.
 *
 * Solo se modelan los campos usados por D06; se ignoran las propiedades que
 * Drive agregue en el futuro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchivoDriveDto(
        String id,
        String name,
        String mimeType,
        Instant modifiedTime) {
}
