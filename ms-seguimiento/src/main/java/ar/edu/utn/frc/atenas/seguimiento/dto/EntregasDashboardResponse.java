package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.util.List;

/**
 * Sección de entregas del dashboard.
 *
 * <p>{@code disponible = false} significa que ms-documentacion no respondió: en ese caso
 * {@code hitos} queda vacío y la respuesta sigue siendo 200 (RN33).</p>
 */
public record EntregasDashboardResponse(boolean disponible, List<Entrega> hitos) {

    /** Estado y nota de un hito, tal como los calcula ms-documentacion (I07). */
    public record Entrega(Long hitoId, String estado, Boolean tardia, Integer nota) {
    }
}
