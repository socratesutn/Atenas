package ar.edu.utn.frc.atenas.seguimiento.dto;

import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;

import java.time.OffsetDateTime;

/** Resumen de un sprint procesado durante una actualización de actividad (S08, RN28). */
public record SnapshotActividadResumen(
        Long hitoId,
        String nombreHito,
        EstadoSnapshot estado,
        OffsetDateTime fechaConsulta,
        Integer commitsTotales,
        Integer commitsSinAtribuir,
        boolean omitido) {
}
