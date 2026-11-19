package ar.edu.utn.frc.atenas.catedra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Participación de un estudiante en un equipo. La fecha de baja queda nula
 * mientras la participación está vigente; el índice único parcial de data.sql
 * impide que un alumno tenga dos participaciones vigentes.
 */
@Entity
@Table(name = "integrante")
public class Integrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(name = "fecha_ingreso", nullable = false)
    private Instant fechaIngreso;

    @Column(name = "fecha_baja")
    private Instant fechaBaja;

    public Integrante() {
        // Constructor requerido por JPA.
    }

    public Integrante(Equipo equipo, Estudiante estudiante, Instant fechaIngreso) {
        this.equipo = equipo;
        this.estudiante = estudiante;
        this.fechaIngreso = fechaIngreso;
    }

    public Integrante(Long id, Equipo equipo, Estudiante estudiante,
                      Instant fechaIngreso, Instant fechaBaja) {
        this.id = id;
        this.equipo = equipo;
        this.estudiante = estudiante;
        this.fechaIngreso = fechaIngreso;
        this.fechaBaja = fechaBaja;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Instant getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Instant fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Instant getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(Instant fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
}
