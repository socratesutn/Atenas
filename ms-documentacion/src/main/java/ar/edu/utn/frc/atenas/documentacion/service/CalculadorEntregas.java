package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combina hitos y equipos para calcular el estado de cada entrega.
 *
 * La regla central de F08 es que PENDIENTE no se persiste: si no hay una fila
 * para (hito, equipo), el estado se calcula al consultar. Este componente es
 * puro y no accede a base de datos, lo que permite probar el cálculo sin red ni
 * Spring.
 */
@Component
public class CalculadorEntregas {

    public static final String PENDIENTE = "PENDIENTE";

    private final ZonaHoraria zonaHoraria;

    public CalculadorEntregas(ZonaHoraria zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    public List<EntregaEstadoResponse> calcular(List<Hito> hitos,
                                                Collection<Long> equipoIds,
                                                List<Entrega> entregas) {
        List<Hito> hitosOrdenados = hitos.stream()
                .sorted(Comparator.comparing(Hito::getOrden)
                        .thenComparing(Hito::getId))
                .toList();

        List<Long> equiposOrdenados = equipoIds.stream()
                .distinct()
                .sorted()
                .toList();

        Map<ClaveEntrega, Entrega> entregasPorClave = new LinkedHashMap<>();
        for (Entrega entrega : entregas) {
            entregasPorClave.put(
                    new ClaveEntrega(entrega.getHito().getId(), entrega.getEquipoId()),
                    entrega);
        }

        return equiposOrdenados.stream()
                .flatMap(equipoId -> hitosOrdenados.stream()
                        .map(hito -> aResponse(equipoId, hito,
                                entregasPorClave.get(new ClaveEntrega(hito.getId(), equipoId)))))
                .toList();
    }

    private EntregaEstadoResponse aResponse(Long equipoId, Hito hito, Entrega entrega) {
        boolean pendiente = entrega == null;
        return new EntregaEstadoResponse(
                equipoId,
                hito.getId(),
                new EntregaEstadoResponse.HitoResumen(
                        hito.getNombre(),
                        hito.getTipo().name(),
                        zonaHoraria.aOffsetDateTime(hito.getFechaInicio()),
                        zonaHoraria.aOffsetDateTime(hito.getFechaLimite())),
                pendiente ? PENDIENTE : entrega.getEstado().name(),
                pendiente ? false : entrega.getTardia(),
                pendiente ? null : entrega.getNota(),
                pendiente ? null : entrega.getId());
    }

    private record ClaveEntrega(Long hitoId, Long equipoId) {
    }
}
