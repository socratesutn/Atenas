package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.ActualizacionActividadResponse;
import ar.edu.utn.frc.atenas.seguimiento.mapper.ActualizacionActividadMapper;
import ar.edu.utn.frc.atenas.seguimiento.service.ActualizacionActividadService;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Actualización de la actividad de GitHub a pedido (S08). */
@Slf4j
@RestController
@RequestMapping("/api/seguimiento")
@RequiredArgsConstructor
@Tag(name = "Actividad", description = "Actualización de la actividad de GitHub por equipo")
public class ActividadController {

    private final ActualizacionActividadService servicio;
    private final ZonaHoraria zonaHoraria;

    @PostMapping("/equipos/{equipoId}/actividad/actualizacion")
    @Operation(summary = "Actualizar la actividad de GitHub de un equipo a pedido",
            description = "Jefe o docente de la comisión. Devuelve el estado de cada sprint; una falla de GitHub "
                    + "o de un servicio interno no se transforma en 503 (RN-28).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de los snapshots resultantes"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o docente de otra comisión (RN-01)"),
            @ApiResponse(responseCode = "404", description = "El equipo no existe o no tiene repositorio vinculado")
    })
    public ResponseEntity<ActualizacionActividadResponse> actualizar(@PathVariable Long equipoId) {
        log.info("S08: actualizar actividad del equipo {}", equipoId);
        ResultadoActualizacion resultado = servicio.actualizarEquipo(equipoId);
        return ResponseEntity.ok(ActualizacionActividadMapper.aResponse(resultado, zonaHoraria));
    }
}
