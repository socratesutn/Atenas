package ar.edu.utn.frc.atenas.documentacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Entrega de un equipo para un hito.
 *
 * En esta historia solo se lee el estado y la nota; la carga del archivo y la
 * corrección se implementan en historias posteriores.
 */
@Entity
@Table(name = "entrega",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entrega_hito_equipo",
                columnNames = {"hito_id", "equipo_id"}))
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hito_id", nullable = false)
    private Hito hito;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estado;

    @Column(nullable = false)
    private Boolean tardia;

    @Column(name = "fecha_entrega")
    private Instant fechaEntrega;

    @Column(name = "registrada_por_sub")
    private String registradaPorSub;

    private Integer nota;

    @Column(name = "modificado_post_entrega")
    private Boolean modificadoPostEntrega;

    @Column(name = "fecha_correccion")
    private Instant fechaCorreccion;

    @Column(name = "corregida_por_sub")
    private String corregidaPorSub;

    public Entrega() {
    }

    public Entrega(Long id, Hito hito, Long equipoId, EstadoEntrega estado,
                   Boolean tardia, Integer nota) {
        this.id = id;
        this.hito = hito;
        this.equipoId = equipoId;
        this.estado = estado;
        this.tardia = tardia;
        this.nota = nota;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hito getHito() {
        return hito;
    }

    public void setHito(Hito hito) {
        this.hito = hito;
    }

    public Long getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(Long equipoId) {
        this.equipoId = equipoId;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public void setEstado(EstadoEntrega estado) {
        this.estado = estado;
    }

    public Boolean getTardia() {
        return tardia;
    }

    public void setTardia(Boolean tardia) {
        this.tardia = tardia;
    }

    public Instant getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(Instant fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getRegistradaPorSub() {
        return registradaPorSub;
    }

    public void setRegistradaPorSub(String registradaPorSub) {
        this.registradaPorSub = registradaPorSub;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public Boolean getModificadoPostEntrega() {
        return modificadoPostEntrega;
    }

    public void setModificadoPostEntrega(Boolean modificadoPostEntrega) {
        this.modificadoPostEntrega = modificadoPostEntrega;
    }

    public Instant getFechaCorreccion() {
        return fechaCorreccion;
    }

    public void setFechaCorreccion(Instant fechaCorreccion) {
        this.fechaCorreccion = fechaCorreccion;
    }

    public String getCorregidaPorSub() {
        return corregidaPorSub;
    }

    public void setCorregidaPorSub(String corregidaPorSub) {
        this.corregidaPorSub = corregidaPorSub;
    }
}
