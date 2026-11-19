package ar.edu.utn.frc.atenas.documentacion.controller;

import ar.edu.utn.frc.atenas.documentacion.dto.CrearHitoRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.HitoResponse;
import ar.edu.utn.frc.atenas.documentacion.service.HitoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** D01 y D02: hitos del curso. */
@RestController
@RequestMapping("/api/documentacion/hitos")
@Tag(name = "Hitos")
public class HitoController {

    private final HitoService hitoService;

    public HitoController(HitoService hitoService) {
        this.hitoService = hitoService;
    }

    @PostMapping
    @Operation(summary = "Define un hito del curso")
    @ApiResponse(responseCode = "201", description = "Hito creado")
    @ApiResponse(responseCode = "400", description = "Datos o fechas inválidas")
    @ApiResponse(responseCode = "403", description = "El usuario no es jefe")
    @ApiResponse(responseCode = "404", description = "El curso no existe")
    @ApiResponse(responseCode = "503", description = "ms-catedra no está disponible")
    public ResponseEntity<HitoResponse> crear(@Valid @RequestBody CrearHitoRequest request) {
        HitoResponse response = hitoService.crear(request);
        return ResponseEntity
                .created(URI.create("/api/documentacion/hitos/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Lista los hitos de un curso")
    @ApiResponse(responseCode = "200", description = "Hitos del curso")
    public List<HitoResponse> listar(@RequestParam Long cursoLectivoId) {
        return hitoService.listar(cursoLectivoId);
    }
}
