package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaAperturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** D06: abrir una entrega y consultar cambios posteriores en Drive. */
@RestController
@RequestMapping("/api/documentacion")
@Tag(name = "Entregas")
public class EntregaAperturaController {

    private final EntregaAperturaService entregaAperturaService;

    public EntregaAperturaController(EntregaAperturaService entregaAperturaService) {
        this.entregaAperturaService = entregaAperturaService;
    }

    @GetMapping("/entregas/{id}")
    @Operation(summary = "Abre una entrega e informa si el archivo cambió después de la entrega y del límite")
    @ApiResponse(responseCode = "200", description = "Entrega abierta; si Drive falla se informa NO_VERIFICADO")
    @ApiResponse(responseCode = "403", description = "El docente no pertenece a la comisión")
    @ApiResponse(responseCode = "404", description = "La entrega no existe")
    @ApiResponse(responseCode = "503", description = "ms-catedra no está disponible")
    public EntregaAperturaResponse abrir(@PathVariable Long id) {
        return entregaAperturaService.abrir(id);
    }
}
