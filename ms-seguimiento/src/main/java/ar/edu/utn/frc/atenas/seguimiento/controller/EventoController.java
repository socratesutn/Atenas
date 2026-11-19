package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.CrearEventoRequest;
import ar.edu.utn.frc.atenas.seguimiento.dto.EventoResponse;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.mapper.EventoMapper;
import ar.edu.utn.frc.atenas.seguimiento.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Calendario de una comisión lectiva (S01 y S02). */
@Slf4j
@RestController
@RequestMapping("/api/seguimiento")
@RequiredArgsConstructor
@Tag(name = "Calendario", description = "Eventos de clase y presentación de una comisión lectiva")
public class EventoController {

    private final EventoService servicio;
    private final ZonaHoraria zonaHoraria;

    @PostMapping("/comisiones-lectivas/{comisionLectivaId}/eventos")
    @Operation(summary = "Crear una clase o presentación en el calendario",
            description = "Solo un docente asignado a la comisión (RN12).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado"),
            @ApiResponse(responseCode = "400", description = "Tipo o fecha ausentes o inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o docente no asignado a la comisión"),
            @ApiResponse(responseCode = "404", description = "La comisión no existe"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public ResponseEntity<EventoResponse> crear(@PathVariable Long comisionLectivaId,
                                                @Valid @RequestBody CrearEventoRequest request) {
        log.info("S01: crear evento {} en la comisión {}", request.tipo(), comisionLectivaId);
        Evento creado = servicio.crear(comisionLectivaId, TipoEvento.valueOf(request.tipo()),
                request.fecha().toInstant(), request.descripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventoMapper.aResponse(creado, zonaHoraria));
    }

    @GetMapping("/comisiones-lectivas/{comisionLectivaId}/eventos")
    @Operation(summary = "Consultar el calendario de una comisión",
            description = "El jefe, un docente de la comisión o un alumno de la comisión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos de la comisión, del más antiguo al más reciente"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "No pertenece a la comisión"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public List<EventoResponse> listar(@PathVariable Long comisionLectivaId) {
        log.info("S02: consultar el calendario de la comisión {}", comisionLectivaId);
        return EventoMapper.aResponses(servicio.listar(comisionLectivaId), zonaHoraria);
    }
}
