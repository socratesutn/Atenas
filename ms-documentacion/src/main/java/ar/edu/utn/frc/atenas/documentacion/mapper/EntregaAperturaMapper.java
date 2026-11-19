package ar.edu.utn.frc.atenas.documentacion.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.dto.CambioPostEntrega;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;

/**
 * Mapea una entrega abierta o recién corregida al contrato D06/D07.
 */
public final class EntregaAperturaMapper {

    private EntregaAperturaMapper() {
    }

    public static EntregaAperturaResponse aResponse(Entrega entrega,
                                                     ArchivoVerificado archivo,
                                                     CambioPostEntrega cambio,
                                                     ZonaHoraria zonaHoraria) {
        Hito hito = entrega.getHito();
        return new EntregaAperturaResponse(
                entrega.getId(),
                new EntregaAperturaResponse.HitoApertura(
                        hito.getId(),
                        hito.getNombre(),
                        hito.getTipo().name(),
                        zonaHoraria.aOffsetDateTime(hito.getFechaInicio()),
                        zonaHoraria.aOffsetDateTime(hito.getFechaLimite())),
                entrega.getEquipoId(),
                entrega.getEstado().name(),
                entrega.getTardia(),
                entrega.getNota(),
                archivo != null ? archivo.getEnlace() : null,
                cambio);
    }
}
