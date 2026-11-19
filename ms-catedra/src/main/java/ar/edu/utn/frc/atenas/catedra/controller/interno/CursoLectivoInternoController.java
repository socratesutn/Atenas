package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.CursoLectivoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno I05: ms-documentacion consulta las fechas del curso para
 * validar un hito. No lleva anotaciones de OpenAPI porque springdoc solo
 * publica las rutas {@code /api/**}.
 */
@RestController
@RequestMapping("/internal/cursos-lectivos")
public class CursoLectivoInternoController {

    private final CursoLectivoService cursoLectivoService;

    public CursoLectivoInternoController(CursoLectivoService cursoLectivoService) {
        this.cursoLectivoService = cursoLectivoService;
    }

    @GetMapping("/{id}")
    public CursoLectivoInternoResponse obtener(@PathVariable Long id) {
        return cursoLectivoService.obtenerInterno(id);
    }
}
