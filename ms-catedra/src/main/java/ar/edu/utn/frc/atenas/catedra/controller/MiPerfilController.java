package ar.edu.utn.frc.atenas.catedra.controller;

import ar.edu.utn.frc.atenas.catedra.dto.ActualizarUsuarioGithubRequest;
import ar.edu.utn.frc.atenas.catedra.dto.MiPerfilResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos C14 y C15 del perfil del alumno autenticado.
 */
@RestController
@RequestMapping("/api/catedra/mi-perfil")
@Tag(name = "Mi perfil", description = "Datos propios del alumno y su usuario de GitHub")
public class MiPerfilController {

    private final EstudianteService estudianteService;

    public MiPerfilController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping
    @Operation(summary = "Consultar el perfil propio del alumno")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del alumno"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no es ALUMNO"),
            @ApiResponse(responseCode = "404", description = "No existe un estudiante asociado al sub del token")
    })
    public MiPerfilResponse obtener() {
        return estudianteService.miPerfil();
    }

    @PutMapping("/usuario-github")
    @Operation(summary = "Actualizar el usuario de GitHub propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario de GitHub actualizado"),
            @ApiResponse(responseCode = "400", description = "Formato de usuario de GitHub inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no es ALUMNO"),
            @ApiResponse(responseCode = "404", description = "No existe un estudiante asociado al sub del token"),
            @ApiResponse(responseCode = "409", description = "El usuario de GitHub ya está en uso")
    })
    public MiPerfilResponse actualizarUsuarioGithub(
            @Valid @RequestBody ActualizarUsuarioGithubRequest request) {
        return estudianteService.actualizarUsuarioGithub(request);
    }
}
