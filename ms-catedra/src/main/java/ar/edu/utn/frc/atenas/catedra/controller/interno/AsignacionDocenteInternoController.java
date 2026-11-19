package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.dto.DocenteAsignadoResponse;
import ar.edu.utn.frc.atenas.catedra.service.AsignacionDocenteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno I04: ms-documentacion y ms-seguimiento consultan si un
 * docente está asignado a una comisión. No aparece en OpenAPI.
 */
@RestController
@RequestMapping("/internal/comisiones-lectivas")
public class AsignacionDocenteInternoController {

    private final AsignacionDocenteService asignacionDocenteService;

    public AsignacionDocenteInternoController(AsignacionDocenteService asignacionDocenteService) {
        this.asignacionDocenteService = asignacionDocenteService;
    }

    @GetMapping("/{id}/docentes/{sub}")
    public DocenteAsignadoResponse estaAsignado(@PathVariable Long id, @PathVariable String sub) {
        return asignacionDocenteService.estaAsignado(id, sub);
    }
}
