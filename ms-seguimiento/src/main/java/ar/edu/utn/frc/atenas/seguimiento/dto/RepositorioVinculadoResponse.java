package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;

/** Repositorio vinculado a un equipo, tal como lo devuelve S07. */
public record RepositorioVinculadoResponse(
        Long equipoId,
        String url,
        String propietario,
        String nombre,
        String rama,
        OffsetDateTime fechaValidacion) {
}
