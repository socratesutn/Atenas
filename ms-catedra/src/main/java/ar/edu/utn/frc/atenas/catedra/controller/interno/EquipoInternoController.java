package ar.edu.utn.frc.atenas.catedra.controller.interno;

import ar.edu.utn.frc.atenas.catedra.dto.EquipoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.service.EquipoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contratos internos I01 e I02. No aparecen en OpenAPI y se consumen dentro de
 * la red de microservicios.
 */
@RestController
@RequestMapping("/internal/equipos")
public class EquipoInternoController {

    private final EquipoService equipoService;

    public EquipoInternoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping("/{id}")
    public EquipoInternoResponse obtener(@PathVariable Long id) {
        return equipoService.obtenerInterno(id);
    }

    @GetMapping
    public List<EquipoInternoResponse> listar(
            @RequestParam(required = false) Long comisionLectivaId,
            @RequestParam(required = false) Long cursoLectivoId) {
        return equipoService.listarInterno(comisionLectivaId, cursoLectivoId);
    }
}
