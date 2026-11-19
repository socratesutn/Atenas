package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionRequest;
import ar.edu.utn.frc.atenas.catedra.service.ComisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Endpoints públicos C03 y C04 para administrar comisiones permanentes.
 */
@RestController
@RequestMapping("/api/catedra/comisiones")
@Tag(name = "Comisiones", description = "Comisiones permanentes que luego se habilitan en un curso lectivo")
public class ComisionController {

    private final ComisionService comisionService;

    public ComisionController(ComisionService comisionService) {
        this.comisionService = comisionService;
    }

    @PostMapping
    @Operation(summary = "Crear una comisión")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comisión creada"),
            @ApiResponse(responseCode = "400", description = "Nombre inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar la estructura"),
            @ApiResponse(responseCode = "409", description = "Ya existe una comisión con ese nombre")
    })
    public ResponseEntity<ComisionResponse> crear(@Valid @RequestBody CrearComisionRequest request) {
        ComisionResponse creada = comisionService.crear(request);
        URI ubicacion = URI.create("/api/catedra/comisiones/" + creada.id());
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar comisiones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de comisiones"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite consultar la estructura")
    })
    public List<ComisionResponse> listar() {
        return comisionService.listar();
    }
}
