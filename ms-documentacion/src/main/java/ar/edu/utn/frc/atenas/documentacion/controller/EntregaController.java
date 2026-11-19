package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas públicas de entregas: D04 y D05. */
@RestController
@RequestMapping("/api/documentacion")
@Tag(name = "Entregas")
public class EntregaController {

    private final EntregaConsultaService entregaConsultaService;

    public EntregaController(EntregaConsultaService entregaConsultaService) {
        this.entregaConsultaService = entregaConsultaService;
    }

    @GetMapping("/entregas")
    @Operation(summary = "Lista entregas y pendientes de una comisión, opcionalmente por hito")
    @ApiResponse(responseCode = "200", description = "Listado de estados de entrega")
    @ApiResponse(responseCode = "403", description = "El docente no pertenece a la comisión")
    @ApiResponse(responseCode = "404", description = "El hito no pertenece al curso")
    @ApiResponse(responseCode = "503", description = "ms-catedra no está disponible")
    public List<EntregaEstadoResponse> listarPorComision(
            @RequestParam Long comisionLectivaId,
            @RequestParam(required = false) Long hitoId) {
        return entregaConsultaService.listarPorComision(comisionLectivaId, hitoId);
    }

    @GetMapping("/mi-equipo/entregas")
    @Operation(summary = "Lista las entregas del equipo del alumno autenticado")
    @ApiResponse(responseCode = "200", description = "Listado de estados de entrega del equipo")
    @ApiResponse(responseCode = "403", description = "El usuario no es alumno")
    @ApiResponse(responseCode = "503", description = "ms-catedra no está disponible")
    public List<EntregaEstadoResponse> listarMiEquipo() {
        return entregaConsultaService.listarMiEquipo();
    }
}
