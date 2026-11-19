package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas puras del cálculo de PENDIENTE (RN-20 y ADR-010). */
class CalculadorEntregasTest {

    private final CalculadorEntregas calculador =
            new CalculadorEntregas(new ZonaHoraria("America/Argentina/Cordoba"));

    @Test
    @DisplayName("RN-20: un equipo sin entregas tiene todos los hitos PENDIENTE")
    void deberiaCalcularPendientes_cuandoElEquipoNoTieneEntregas() {
        Hito hito = hito(3L, 1, "Sprint 1");

        List<EntregaEstadoResponse> resultado = calculador.calcular(
                List.of(hito), List.of(7L), List.of());

        assertThat(resultado).hasSize(1);
        EntregaEstadoResponse respuesta = resultado.getFirst();
        assertThat(respuesta.equipoId()).isEqualTo(7L);
        assertThat(respuesta.hitoId()).isEqualTo(3L);
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.tardia()).isFalse();
        assertThat(respuesta.nota()).isNull();
        assertThat(respuesta.entregaId()).isNull();
        assertThat(respuesta.hito().nombre()).isEqualTo("Sprint 1");
        assertThat(respuesta.hito().tipo()).isEqualTo("SPRINT");
        assertThat(respuesta.hito().fechaInicio())
                .isEqualTo(OffsetDateTime.parse("2026-03-01T00:00:00-03:00"));
    }

    @Test
    @DisplayName("F08: calcula ENTREGADA y CORREGIDA junto con sus datos")
    void deberiaCombinarEntregadaYCorregida_cuandoExistenFilas() {
        Hito hito1 = hito(3L, 1, "Sprint 1");
        Hito hito2 = hito(4L, 2, "Sprint 2");
        Entrega entregada = entrega(30L, hito1, 7L, EstadoEntrega.ENTREGADA, true, null);
        Entrega corregida = entrega(31L, hito2, 7L, EstadoEntrega.CORREGIDA, false, 8);

        List<EntregaEstadoResponse> resultado = calculador.calcular(
                List.of(hito2, hito1), List.of(7L), List.of(corregida, entregada));

        assertThat(resultado).extracting(EntregaEstadoResponse::hitoId).containsExactly(3L, 4L);
        assertThat(resultado.get(0).estado()).isEqualTo("ENTREGADA");
        assertThat(resultado.get(0).tardia()).isTrue();
        assertThat(resultado.get(0).nota()).isNull();
        assertThat(resultado.get(0).entregaId()).isEqualTo(30L);
        assertThat(resultado.get(1).estado()).isEqualTo("CORREGIDA");
        assertThat(resultado.get(1).tardia()).isFalse();
        assertThat(resultado.get(1).nota()).isEqualTo(8);
        assertThat(resultado.get(1).entregaId()).isEqualTo(31L);
    }

    @Test
    @DisplayName("ADR-010: un equipo sin fila sigue PENDIENTE aunque el hito sea anterior")
    void deberiaIncluirPendiente_cuandoElEquipoNoTieneEntregaParaUnHitoAnterior() {
        Hito hito = hito(3L, 1, "Sprint 1");
        Entrega entregaEquipo7 = entrega(30L, hito, 7L, EstadoEntrega.ENTREGADA, false, null);

        List<EntregaEstadoResponse> resultado = calculador.calcular(
                List.of(hito), List.of(8L, 7L), List.of(entregaEquipo7));

        assertThat(resultado).extracting(EntregaEstadoResponse::equipoId).containsExactly(7L, 8L);
        assertThat(resultado.get(1).estado()).isEqualTo("PENDIENTE");
        assertThat(resultado.get(1).entregaId()).isNull();
    }

    private Hito hito(Long id, int orden, String nombre) {
        return new Hito(id, 2L, nombre, TipoHito.SPRINT, orden,
                Instant.parse("2026-03-01T03:00:00Z"),
                Instant.parse("2026-03-16T02:59:59Z"));
    }

    private Entrega entrega(Long id, Hito hito, Long equipoId, EstadoEntrega estado,
                            boolean tardia, Integer nota) {
        return new Entrega(id, hito, equipoId, estado, tardia, nota);
    }
}
