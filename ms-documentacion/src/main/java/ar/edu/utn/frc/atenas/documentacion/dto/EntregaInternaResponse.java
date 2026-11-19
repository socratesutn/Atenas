package ar.edu.utn.frc.atenas.documentacion.dto;

/**
 * Respuesta de I07: contrato mínimo acordado en docs/06.
 *
 * No incluye los datos del hito ni el id de la entrega porque el consumidor
 * (ms-seguimiento) no los necesita para calcular actividad y dashboard.
 */
public record EntregaInternaResponse(
        Long equipoId,
        Long hitoId,
        String estado,
        Boolean tardia,
        Integer nota) {

    public static EntregaInternaResponse desde(EntregaEstadoResponse response) {
        return new EntregaInternaResponse(
                response.equipoId(),
                response.hitoId(),
                response.estado(),
                response.tardia(),
                response.nota());
    }
}
