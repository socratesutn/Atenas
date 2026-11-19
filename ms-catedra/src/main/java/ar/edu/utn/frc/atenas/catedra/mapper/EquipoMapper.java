package ar.edu.utn.frc.atenas.catedra.mapper;

import ar.edu.utn.frc.atenas.catedra.dto.EquipoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResumenResponse;
import ar.edu.utn.frc.atenas.catedra.dto.IntegranteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.IntegranteResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.entity.Integrante;

import java.util.List;

/**
 * Conversión manual de equipos a los DTO públicos e internos.
 */
public final class EquipoMapper {

    private EquipoMapper() {
    }

    public static EquipoResponse aResponse(Equipo equipo, List<Integrante> integrantes) {
        int cantidad = integrantes.size();
        return new EquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getComisionLectiva().getId(),
                equipo.getComisionLectiva().getCursoLectivo().getId(),
                equipo.getEstado().name(),
                equipo.isAsegurado(),
                equipo.getCupoMinimo(),
                equipo.getCupoMaximo(),
                cantidad,
                disponible(equipo, cantidad),
                representanteSub(equipo),
                integrantes.stream().map(EquipoMapper::aIntegrante).toList());
    }

    public static EquipoResumenResponse aResumen(Equipo equipo, int cantidadIntegrantes) {
        return new EquipoResumenResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getComisionLectiva().getId(),
                equipo.getComisionLectiva().getCursoLectivo().getId(),
                equipo.getEstado().name(),
                equipo.isAsegurado(),
                equipo.getCupoMinimo(),
                equipo.getCupoMaximo(),
                cantidadIntegrantes,
                disponible(equipo, cantidadIntegrantes),
                representanteSub(equipo));
    }

    public static EquipoInternoResponse aInterno(Equipo equipo, List<Integrante> integrantes) {
        return new EquipoInternoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getComisionLectiva().getId(),
                equipo.getComisionLectiva().getCursoLectivo().getId(),
                equipo.getEstado().name(),
                equipo.isAsegurado(),
                representanteSub(equipo),
                integrantes.stream().map(EquipoMapper::aIntegranteInterno).toList());
    }

    private static IntegranteResponse aIntegrante(Integrante integrante) {
        return new IntegranteResponse(
                integrante.getEstudiante().getId(),
                integrante.getEstudiante().getSub(),
                integrante.getEstudiante().getLegajo(),
                integrante.getEstudiante().getUsuarioGithub());
    }

    private static IntegranteInternoResponse aIntegranteInterno(Integrante integrante) {
        return new IntegranteInternoResponse(
                integrante.getEstudiante().getId(),
                integrante.getEstudiante().getSub(),
                integrante.getEstudiante().getLegajo(),
                integrante.getEstudiante().getUsuarioGithub());
    }

    private static boolean disponible(Equipo equipo, int cantidadIntegrantes) {
        return !equipo.isAsegurado() && cantidadIntegrantes < equipo.getCupoMaximo();
    }

    private static String representanteSub(Equipo equipo) {
        return equipo.getRepresentante() == null ? null : equipo.getRepresentante().getSub();
    }
}
