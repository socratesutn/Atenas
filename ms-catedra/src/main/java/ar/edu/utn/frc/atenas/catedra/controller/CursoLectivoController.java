package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.CrearCursoLectivoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoResponse;
import ar.edu.utn.frc.atenas.catedra.service.CursoLectivoService;
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
 * Endpoints públicos C01 y C02 para administrar cursos lectivos.
 */
@RestController
@RequestMapping("/api/catedra/cursos-lectivos")
@Tag(name = "Cursos lectivos", description = "Administración de los años académicos de la cátedra")
public class CursoLectivoController {

    private final CursoLectivoService cursoLectivoService;

    public CursoLectivoController(CursoLectivoService cursoLectivoService) {
        this.cursoLectivoService = cursoLectivoService;
    }

    @PostMapping
    @Operation(summary = "Crear un curso lectivo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curso lectivo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rango de fechas incorrecto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite administrar la estructura"),
            @ApiResponse(responseCode = "409", description = "Ya existe un curso lectivo con ese año")
    })
    public ResponseEntity<CursoLectivoResponse> crear(@Valid @RequestBody CrearCursoLectivoRequest request) {
        CursoLectivoResponse creado = cursoLectivoService.crear(request);
        URI ubicacion = URI.create("/api/catedra/cursos-lectivos/" + creado.id());
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar cursos lectivos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de cursos lectivos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no permite consultar la estructura")
    })
    public List<CursoLectivoResponse> listar() {
        return cursoLectivoService.listar();
    }
}
