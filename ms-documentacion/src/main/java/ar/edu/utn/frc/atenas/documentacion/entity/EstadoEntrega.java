package ar.edu.utn.frc.atenas.documentacion.entity;

/**
 * Estados que se persisten para una entrega.
 *
 * PENDIENTE no existe en la base: se calcula al consultar cuando no hay fila
 * para la combinación hito/equipo.
 */
public enum EstadoEntrega {
    ENTREGADA,
    CORREGIDA
}
