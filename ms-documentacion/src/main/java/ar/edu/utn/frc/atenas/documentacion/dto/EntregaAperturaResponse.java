package ar.edu.utn.frc.atenas.documentacion.dto;

import java.time.OffsetDateTime;

/**
 * Respuesta de D06 al abrir una entrega.
 *
 * Incluye el hito completo y el resultado informativo de RN-30. El valor
 * {@code NO_VERIFICADO} aparece cuando Drive no pudo consultarse o no hay un
 * valor congelado disponible.
 */
public record EntregaAperturaResponse(
        Long id,
        HitoApertura hito,
        Long equipoId,
        String estado,
        Boolean tardia,
        Integer nota,
        String enlace,
        CambioPostEntrega modificadoPostEntrega) {

    /** Datos completos del hito que se muestran al abrir la entrega. */
    public record HitoApertura(
            Long id,
            String nombre,
            String tipo,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaLimite) {
    }
}
