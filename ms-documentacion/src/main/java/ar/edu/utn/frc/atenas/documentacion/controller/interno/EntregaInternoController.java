package ar.edu.utn.frc.atenas.documentacion.controller.interno;

import ar.edu.utn.frc.atenas.documentacion.dto.EntregaInternaResponse;
import ar.edu.utn.frc.atenas.documentacion.service.EntregaConsultaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrato interno I07.
 *
 * No lleva anotaciones de OpenAPI porque springdoc solo publica las rutas
 * /api/**. El Gateway tampoco expone /internal/**.
 */
@RestController
@RequestMapping("/internal/entregas")
public class EntregaInternoController {

    private final EntregaConsultaService entregaConsultaService;

    public EntregaInternoController(EntregaConsultaService entregaConsultaService) {
        this.entregaConsultaService = entregaConsultaService;
    }

    @GetMapping
    public List<EntregaInternaResponse> listar(
            @RequestParam Long cursoLectivoId,
            @RequestParam List<Long> equipoIds) {
        return entregaConsultaService.listarInterno(cursoLectivoId, equipoIds);
    }
}
