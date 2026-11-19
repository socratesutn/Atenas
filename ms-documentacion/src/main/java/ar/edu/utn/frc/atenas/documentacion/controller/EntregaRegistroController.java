package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.documentacion.dto.EntregaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.RegistrarEntregaRequest;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaRegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** D03: registrar entrega verificada en Drive. */
@RestController
@RequestMapping("/api/documentacion/hitos")
@Tag(name = "Entregas")
public class EntregaRegistroController {

    private final EntregaRegistroService entregaRegistroService;

    public EntregaRegistroController(EntregaRegistroService entregaRegistroService) {
        this.entregaRegistroService = entregaRegistroService;
    }

    @PostMapping("/{id}/entregas")
    @Operation(summary = "Registra una entrega verificada en Drive")
    @ApiResponse(responseCode = "201", description = "Entrega registrada")
    @ApiResponse(responseCode = "400", description = "Enlace inválido")
    @ApiResponse(responseCode = "403", description = "El alumno no es representante o no pertenece al curso")
    @ApiResponse(responseCode = "409", description = "La entrega ya existe o el equipo no está asegurado")
    @ApiResponse(responseCode = "422", description = "Archivo no accesible o tipo no permitido")
    @ApiResponse(responseCode = "503", description = "ms-catedra o Drive no disponibles")
    public ResponseEntity<EntregaResponse> registrar(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarEntregaRequest request) {
        EntregaResponse response = entregaRegistroService.registrar(id, request);
        return ResponseEntity
                .created(URI.create("/api/documentacion/entregas/" + response.id()))
                .body(response);
    }
}
