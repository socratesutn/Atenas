package ar.edu.utn.frc.atenas.seguimiento.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.ActualizacionActividadResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.SnapshotActividadResumen;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion;

import java.util.List;

/** Convierte el resultado interno de la actualización en el contrato JSON de S08. */
public final class ActualizacionActividadMapper {

    private ActualizacionActividadMapper() {
    }

    public static ActualizacionActividadResponse aResponse(ResultadoActualizacion resultado, ZonaHoraria zonaHoraria) {
        List<SnapshotActividadResumen> snapshots = resultado.detalles().stream()
                .map(detalle -> new SnapshotActividadResumen(
                        detalle.hitoId(),
                        detalle.nombreHito(),
                        detalle.snapshot().getEstado(),
                        zonaHoraria.aOffsetDateTime(detalle.snapshot().getFechaConsulta()),
                        detalle.snapshot().getCommitsTotales(),
                        detalle.snapshot().getCommitsSinAtribuir(),
                        detalle.omitido()))
                .toList();
        return new ActualizacionActividadResponse(
                resultado.equipoId(), zonaHoraria.aOffsetDateTime(resultado.fechaConsulta()), snapshots);
    }
}
