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
 * Asignación de un docente a una comisión lectiva. La combinación es única:
 * el mismo docente no se asigna dos veces a la misma comisión.
 */
@Entity
@Table(
        name = "asignacion_docente",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asignacion_docente_docente_comision",
                columnNames = {"docente_id", "comision_lectiva_id"}))
public class AsignacionDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_lectiva_id", nullable = false)
    private ComisionLectiva comisionLectiva;

    public AsignacionDocente() {
        // Constructor requerido por JPA.
    }

    public AsignacionDocente(Docente docente, ComisionLectiva comisionLectiva) {
        this.docente = docente;
        this.comisionLectiva = comisionLectiva;
    }

    public AsignacionDocente(Long id, Docente docente, ComisionLectiva comisionLectiva) {
        this.id = id;
        this.docente = docente;
        this.comisionLectiva = comisionLectiva;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }

    public ComisionLectiva getComisionLectiva() {
        return comisionLectiva;
    }

    public void setComisionLectiva(ComisionLectiva comisionLectiva) {
        this.comisionLectiva = comisionLectiva;
    }
}
