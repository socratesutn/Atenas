package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.AsignacionDocenteResponse;
import ar.edu.utn.frc.atenas.catedra.dto.AsignarDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.service.AsignacionDocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoint público C10: asignar un docente existente a una comisión lectiva.
 */
@RestController
@RequestMapping("/api/catedra/comisiones-lectivas")
@Tag(name = "Asignación de docentes", description = "Asignación de docentes a comisiones lectivas")
public class AsignacionDocenteController {

    private final AsignacionDocenteService asignacionDocenteService;

    public AsignacionDocenteController(AsignacionDocenteService asignacionDocenteService) {
        this.asignacionDocenteService = asignacionDocenteService;
    }

    @PostMapping("/{id}/docentes")
    @Operation(summary = "Asignar un docente a una comisión lectiva")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Docente asignado a la comisión"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite asignar docentes"),
            @ApiResponse(responseCode = "404", description = "No existe la comisión lectiva o el docente"),
            @ApiResponse(responseCode = "409", description = "El docente ya está asignado a la comisión")
    })
    public ResponseEntity<AsignacionDocenteResponse> asignar(
            @PathVariable Long id,
            @Valid @RequestBody AsignarDocenteRequest request) {
        AsignacionDocenteResponse creada = asignacionDocenteService.asignar(id, request);
        URI ubicacion = URI.create("/api/catedra/comisiones-lectivas/" + id
                + "/docentes/" + creada.docenteId());
        return ResponseEntity.created(ubicacion).body(creada);
    }
}
