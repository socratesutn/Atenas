package ar.edu.utn.frc.atenas.documentacion.controller.interno;

import ar.edu.utn.frc.atenas.documentacion.dto.HitoResponse;
import ar.edu.utn.frc.atenas.documentacion.service.HitoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * I06: hitos de un curso para ms-seguimiento.
 *
 * No aparece en OpenAPI porque springdoc solo publica /api/**.
 */
@RestController
@RequestMapping("/internal/hitos")
public class HitoInternoController {

    private final HitoService hitoService;

    public HitoInternoController(HitoService hitoService) {
        this.hitoService = hitoService;
    }

    @GetMapping
    public List<HitoResponse> listar(@RequestParam Long cursoLectivoId,
                                     @RequestParam(required = false) String tipo) {
        return hitoService.internos(cursoLectivoId, tipo);
    }
}
