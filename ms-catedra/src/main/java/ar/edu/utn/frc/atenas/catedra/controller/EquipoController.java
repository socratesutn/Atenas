package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.CrearEquipoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResumenResponse;
import ar.edu.utn.frc.atenas.catedra.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints públicos C16-C19 para crear, consultar e ingresar a equipos.
 */
@RestController
@RequestMapping("/api/catedra")
@Tag(name = "Equipos", description = "Espacios de equipo, disponibilidad e ingreso de alumnos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @PostMapping("/comisiones-lectivas/{id}/equipos")
    @Operation(summary = "Crear un espacio de equipo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipo creado"),
            @ApiResponse(responseCode = "400", description = "Cupos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El docente no está asignado a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe la comisión lectiva"),
            @ApiResponse(responseCode = "409", description = "Ya existe un equipo con ese nombre en la comisión")
    })
    public ResponseEntity<EquipoResponse> crear(@PathVariable Long id,
                                                @Valid @RequestBody CrearEquipoRequest request) {
        EquipoResponse creado = equipoService.crear(id, request);
        return ResponseEntity.created(URI.create("/api/catedra/equipos/" + creado.id())).body(creado);
    }

    @GetMapping("/comisiones-lectivas/{id}/equipos")
    @Operation(summary = "Listar equipos de una comisión y su disponibilidad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipos de la comisión"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no pertenece a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe la comisión lectiva")
    })
    public List<EquipoResumenResponse> listar(@PathVariable Long id) {
        return equipoService.listarPorComision(id);
    }

    @GetMapping("/equipos/{id}")
    @Operation(summary = "Consultar un equipo con sus integrantes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del equipo"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no pertenece a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe el equipo")
    })
    public EquipoResponse obtener(@PathVariable Long id) {
        return equipoService.obtener(id);
    }

    @PostMapping("/equipos/{id}/integrantes")
    @Operation(summary = "Ingresar al equipo con la identidad del alumno autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingreso registrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El alumno no pertenece a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe el equipo o el estudiante"),
            @ApiResponse(responseCode = "409", description = "Equipo asegurado, sin cupo o alumno ya integrado")
    })
    public ResponseEntity<EquipoResponse> ingresar(@PathVariable Long id) {
        EquipoResponse actualizado = equipoService.ingresar(id);
        return ResponseEntity.created(URI.create("/api/catedra/equipos/" + id)).body(actualizado);
    }

    @DeleteMapping("/equipos/{id}/integrantes/{estudianteId}")
    @Operation(summary = "Salir del equipo o rechazar un ingreso")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participación cerrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "No es el propio integrante ni el representante"),
            @ApiResponse(responseCode = "404", description = "No existe el equipo o la participación vigente"),
            @ApiResponse(responseCode = "409", description = "El equipo está asegurado")
    })
    public ResponseEntity<Void> salir(@PathVariable Long id,
                                     @PathVariable Long estudianteId) {
        equipoService.salir(id, estudianteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/equipos/{id}/aseguramiento")
    @Operation(summary = "Asegurar un equipo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipo asegurado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El alumno no es representante o el docente no está asignado"),
            @ApiResponse(responseCode = "404", description = "No existe el equipo"),
            @ApiResponse(responseCode = "409", description = "El equipo ya está asegurado o no cumple las condiciones")
    })
    public EquipoResponse asegurar(@PathVariable Long id) {
        return equipoService.asegurar(id);
    }

    @PatchMapping("/equipos/{id}/reapertura")
    @Operation(summary = "Reabrir un equipo asegurado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipo reabierto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El docente no está asignado a la comisión"),
            @ApiResponse(responseCode = "404", description = "No existe el equipo"),
            @ApiResponse(responseCode = "409", description = "El equipo ya está abierto")
    })
    public EquipoResponse reabrir(@PathVariable Long id) {
        return equipoService.reabrir(id);
    }
}
