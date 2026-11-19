package ar.edu.utn.frc.atenas.seguimiento.mapper;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.EventoResponse;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;

import java.util.List;

/** Convierte {@link Evento} en su respuesta de la API. */
public final class EventoMapper {

    private EventoMapper() {
    }

    public static EventoResponse aResponse(Evento evento, ZonaHoraria zonaHoraria) {
        return new EventoResponse(
                evento.getId(),
                evento.getComisionLectivaId(),
                evento.getTipo().name(),
                zonaHoraria.aOffsetDateTime(evento.getFecha()),
                evento.getDescripcion());
    }

    public static List<EventoResponse> aResponses(List<Evento> eventos, ZonaHoraria zonaHoraria) {
        return eventos.stream().map(evento -> aResponse(evento, zonaHoraria)).toList();
    }
}
