package ar.edu.utn.frc.atenas.documentacion.dto;

import java.time.OffsetDateTime;

/**
 * Estado de una entrega para un equipo y un hito.
 *
 * Si no existe fila persistida, el estado es PENDIENTE y los campos de la
 * entrega quedan nulos. Es el DTO que usan D04 y D05.
 */
public record EntregaEstadoResponse(
        Long equipoId,
        Long hitoId,
        HitoResumen hito,
        String estado,
        Boolean tardia,
        Integer nota,
        Long entregaId) {

    /** Datos mínimos del hito que se muestran junto al estado. */
    public record HitoResumen(
            String nombre,
            String tipo,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaLimite) {
    }
}
