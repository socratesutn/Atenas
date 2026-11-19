package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.CrearDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteResponse;
import ar.edu.utn.frc.atenas.catedra.service.DocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Endpoints públicos C07, C08 y C09 para administrar docentes y sus cuentas
 * de Keycloak.
 */
@RestController
@RequestMapping("/api/catedra/docentes")
@Tag(name = "Docentes", description = "Alta, listado y baja lógica de docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @PostMapping
    @Operation(summary = "Registrar un docente y crear su cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Docente creado y cuenta de Keycloak asociada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar docentes"),
            @ApiResponse(responseCode = "409", description = "Ya existe un docente o usuario con ese mail"),
            @ApiResponse(responseCode = "503", description = "Keycloak no respondió o rechazó la operación")
    })
    public ResponseEntity<DocenteResponse> crear(@Valid @RequestBody CrearDocenteRequest request) {
        DocenteResponse creado = docenteService.crear(request);
        URI ubicacion = URI.create("/api/catedra/docentes/" + creado.id());
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar docentes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de docentes"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite consultar docentes")
    })
    public List<DocenteResponse> listar() {
        return docenteService.listar();
    }

    @PatchMapping("/{id}/baja")
    @Operation(summary = "Dar de baja lógicamente a un docente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Docente dado de baja"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar docentes"),
            @ApiResponse(responseCode = "404", description = "No existe el docente"),
            @ApiResponse(responseCode = "503", description = "Keycloak no respondió o rechazó la baja")
    })
    public DocenteResponse darDeBaja(@PathVariable Long id) {
        return docenteService.darDeBaja(id);
    }
}
