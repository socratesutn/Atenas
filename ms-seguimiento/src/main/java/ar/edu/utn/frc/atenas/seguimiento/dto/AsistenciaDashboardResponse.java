package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Sección de asistencia de S10: totales del equipo y detalle de cada registro. */
public record AsistenciaDashboardResponse(
        int eventos,
        int registros,
        int presentes,
        List<Detalle> detalle) {

    /** Una fila de asistencia, con el evento al que corresponde. */
    public record Detalle(
            Long eventoId,
            OffsetDateTime fecha,
            String tipo,
            String estudianteSub,
            boolean presente) {
    }
}
