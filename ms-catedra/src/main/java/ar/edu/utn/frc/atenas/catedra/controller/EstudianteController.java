package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.CrearEstudianteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
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
 * Endpoints públicos C11, C12 y C13 para administrar estudiantes.
 */
@RestController
@RequestMapping("/api/catedra")
@Tag(name = "Estudiantes", description = "Alta, listado por comisión y baja lógica de estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @PostMapping("/estudiantes")
    @Operation(summary = "Registrar un estudiante y crear su cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante creado y cuenta de Keycloak asociada"),
            @ApiResponse(responseCode = "400", description = "Legajo o datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar estudiantes"),
            @ApiResponse(responseCode = "404", description = "No existe la comisión lectiva"),
            @ApiResponse(responseCode = "409", description = "Legajo o usuario de Keycloak duplicado"),
            @ApiResponse(responseCode = "503", description = "Keycloak no respondió o rechazó la operación")
    })
    public ResponseEntity<EstudianteResponse> crear(@Valid @RequestBody CrearEstudianteRequest request) {
        EstudianteResponse creado = estudianteService.crear(request);
        URI ubicacion = URI.create("/api/catedra/estudiantes/" + creado.id());
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/comisiones-lectivas/{id}/estudiantes")
    @Operation(summary = "Listar estudiantes de una comisión lectiva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiantes de la comisión"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El docente no está asignado a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe la comisión lectiva")
    })
    public List<EstudianteResponse> listar(@PathVariable Long id) {
        return estudianteService.listarPorComision(id);
    }

    @PatchMapping("/estudiantes/{id}/baja")
    @Operation(summary = "Dar de baja lógicamente a un estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante dado de baja"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar estudiantes"),
            @ApiResponse(responseCode = "404", description = "No existe el estudiante"),
            @ApiResponse(responseCode = "409", description = "El equipo del estudiante impide la baja"),
            @ApiResponse(responseCode = "503", description = "Keycloak no respondió o rechazó la baja")
    })
    public EstudianteResponse darDeBaja(@PathVariable Long id) {
        return estudianteService.darDeBaja(id);
    }
}
