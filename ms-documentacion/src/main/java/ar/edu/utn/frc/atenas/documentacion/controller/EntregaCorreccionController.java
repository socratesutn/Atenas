package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.documentacion.dto.CorregirEntregaRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaAperturaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaCorreccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** D07: corregir una entrega entregada. */
@RestController
@RequestMapping("/api/documentacion")
@Tag(name = "Entregas")
public class EntregaCorreccionController {

    private final EntregaCorreccionService entregaCorreccionService;

    public EntregaCorreccionController(EntregaCorreccionService entregaCorreccionService) {
        this.entregaCorreccionService = entregaCorreccionService;
    }

    @PatchMapping("/entregas/{id}/correccion")
    @Operation(summary = "Corrige una entrega con una nota entera de 1 a 10")
    @ApiResponse(responseCode = "200", description = "Entrega corregida")
    @ApiResponse(responseCode = "403", description = "El docente no pertenece a la comisión")
    @ApiResponse(responseCode = "404", description = "La entrega no existe")
    @ApiResponse(responseCode = "409", description = "La entrega ya está corregida")
    @ApiResponse(responseCode = "422", description = "La nota no es un entero entre 1 y 10")
    @ApiResponse(responseCode = "503", description = "ms-catedra no está disponible")
    public EntregaAperturaResponse corregir(@PathVariable Long id,
                                             @RequestBody CorregirEntregaRequest request) {
        return entregaCorreccionService.corregir(id, request.nota());
    }
}
