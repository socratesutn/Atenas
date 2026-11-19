package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.dto.EstudianteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.EstudianteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno I03: ms-documentacion y ms-seguimiento consultan la
 * pertenencia vigente de un alumno por su sub. No aparece en OpenAPI.
 */
@RestController
@RequestMapping("/internal/estudiantes")
public class EstudianteInternoController {

    private final EstudianteService estudianteService;

    public EstudianteInternoController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping("/por-sub/{sub}")
    public EstudianteInternoResponse obtenerPorSub(@PathVariable String sub) {
        return estudianteService.obtenerInternoPorSub(sub);
    }
}
