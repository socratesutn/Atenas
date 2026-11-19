package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.RepositorioVinculadoResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.VincularRepositorioRequest;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.mapper.RepositorioVinculadoMapper;
import ar.edu.utn.frc.atenas.seguimiento.service.RepositorioVinculadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vinculación del repositorio de GitHub de un equipo (S07). */
@Slf4j
@RestController
@RequestMapping("/api/seguimiento")
@RequiredArgsConstructor
@Tag(name = "Repositorio", description = "Vinculación del repositorio de GitHub de un equipo")
public class RepositorioController {

    private final RepositorioVinculadoService servicio;
    private final ZonaHoraria zonaHoraria;

    @PutMapping("/equipos/{equipoId}/repositorio")
    @Operation(summary = "Vincular y validar el repositorio y la rama de seguimiento de un equipo",
            description = "Solo el representante del equipo. Si el equipo ya tenía un repositorio vinculado, lo reemplaza.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repositorio validado y vinculado"),
            @ApiResponse(responseCode = "400", description = "URL o rama ausentes o con formato inválido"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o el usuario no es el representante (RN-25)"),
            @ApiResponse(responseCode = "404", description = "El equipo no existe"),
            @ApiResponse(responseCode = "422", description = "El repositorio no existe, no es público o la rama no existe (RN-25)"),
            @ApiResponse(responseCode = "503", description = "ms-catedra o GitHub no responden")
    })
    public ResponseEntity<RepositorioVinculadoResponse> vincular(@PathVariable Long equipoId,
                                                                 @Valid @RequestBody VincularRepositorioRequest request) {
        log.info("S07: vincular repositorio al equipo {}", equipoId);
        RepositorioVinculado vinculado = servicio.vincular(equipoId, request.url(), request.rama());
        return ResponseEntity.ok(RepositorioVinculadoMapper.aResponse(vinculado, zonaHoraria));
    }
}
