package ar.edu.utn.frc.atenas.documentacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Hito de un curso lectivo.
 *
 * El curso pertenece a ms-catedra, por eso se guarda solo su id y no una
 * relación JPA entre esquemas.
 */
@Entity
@Table(name = "hito")
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_lectivo_id", nullable = false)
    private Long cursoLectivoId;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoHito tipo;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_limite", nullable = false)
    private Instant fechaLimite;

    public Hito() {
    }

    public Hito(Long id, Long cursoLectivoId, String nombre, TipoHito tipo,
                Integer orden, Instant fechaInicio, Instant fechaLimite) {
        this.id = id;
        this.cursoLectivoId = cursoLectivoId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.orden = orden;
        this.fechaInicio = fechaInicio;
        this.fechaLimite = fechaLimite;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCursoLectivoId() {
        return cursoLectivoId;
    }

    public void setCursoLectivoId(Long cursoLectivoId) {
        this.cursoLectivoId = cursoLectivoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoHito getTipo() {
        return tipo;
    }

    public void setTipo(TipoHito tipo) {
        this.tipo = tipo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Instant fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Instant getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(Instant fechaLimite) {
        this.fechaLimite = fechaLimite;
    }
}
