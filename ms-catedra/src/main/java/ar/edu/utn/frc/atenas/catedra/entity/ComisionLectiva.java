package ar.edu.utn.frc.atenas.catedra.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Dictado concreto de una comisión en un curso lectivo. La combinación de
 * comisión y curso es única: una comisión no se habilita dos veces en el mismo
 * año.
 */
@Entity
@Table(
        name = "comision_lectiva",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comision_lectiva_comision_curso",
                columnNames = {"comision_id", "curso_lectivo_id"}))
public class ComisionLectiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_id", nullable = false)
    private Comision comision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_lectivo_id", nullable = false)
    private CursoLectivo cursoLectivo;

    public ComisionLectiva() {
        // Constructor requerido por JPA.
    }

    public ComisionLectiva(Comision comision, CursoLectivo cursoLectivo) {
        this.comision = comision;
        this.cursoLectivo = cursoLectivo;
    }

    public ComisionLectiva(Long id, Comision comision, CursoLectivo cursoLectivo) {
        this.id = id;
        this.comision = comision;
        this.cursoLectivo = cursoLectivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Comision getComision() {
        return comision;
    }

    public void setComision(Comision comision) {
        this.comision = comision;
    }

    public CursoLectivo getCursoLectivo() {
        return cursoLectivo;
    }

    public void setCursoLectivo(CursoLectivo cursoLectivo) {
        this.cursoLectivo = cursoLectivo;
    }
}
