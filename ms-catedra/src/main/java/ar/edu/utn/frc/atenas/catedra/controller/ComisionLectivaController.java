package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionLectivaResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionLectivaRequest;
import ar.edu.utn.frc.atenas.catedra.service.ComisionLectivaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Endpoints públicos C05 y C06: habilitar y listar comisiones dentro de un
 * curso lectivo.
 */
@RestController
@RequestMapping("/api/catedra/cursos-lectivos")
@Tag(name = "Comisiones lectivas", description = "Comisiones habilitadas dentro de cada curso lectivo")
public class ComisionLectivaController {

    private final ComisionLectivaService comisionLectivaService;

    public ComisionLectivaController(ComisionLectivaService comisionLectivaService) {
        this.comisionLectivaService = comisionLectivaService;
    }

    @PostMapping("/{id}/comisiones-lectivas")
    @Operation(summary = "Habilitar una comisión en un curso lectivo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comisión habilitada en el curso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar la estructura"),
            @ApiResponse(responseCode = "404", description = "No existe el curso lectivo o la comisión"),
            @ApiResponse(responseCode = "409", description = "La comisión ya está habilitada en el curso")
    })
    public ResponseEntity<ComisionLectivaResponse> crear(@PathVariable Long id,
                                                         @Valid @RequestBody CrearComisionLectivaRequest request) {
        ComisionLectivaResponse creada = comisionLectivaService.crear(id, request);
        URI ubicacion = URI.create("/api/catedra/cursos-lectivos/" + id
                + "/comisiones-lectivas/" + creada.id());
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/{id}/comisiones-lectivas")
    @Operation(summary = "Listar las comisiones lectivas de un curso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comisiones del curso"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite consultar la estructura"),
            @ApiResponse(responseCode = "404", description = "No existe el curso lectivo")
    })
    public List<ComisionLectivaResponse> listar(@PathVariable Long id) {
        return comisionLectivaService.listarPorCurso(id);
    }
}
