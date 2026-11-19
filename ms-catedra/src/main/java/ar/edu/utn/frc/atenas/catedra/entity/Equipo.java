package ar.edu.utn.frc.atenas.catedra.entity;

import ar.edu.utn.frc.atenas.catedra.estado.EstadoEquipo;
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

/**
 * Espacio de trabajo de una comisión lectiva. Nace ABIERTO y su comportamiento
 * depende del estado persistido; el representante se asigna al primer ingreso.
 */
@Entity
@Table(
        name = "equipo",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_equipo_comision_nombre",
                columnNames = {"comision_lectiva_id", "nombre"}))
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_lectiva_id", nullable = false)
    private ComisionLectiva comisionLectiva;

    @Column(name = "cupo_minimo", nullable = false)
    private Integer cupoMinimo;

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEquipo estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representante_id", unique = true)
    private Estudiante representante;

    public Equipo() {
        // Constructor requerido por JPA.
    }

    public Equipo(String nombre, Integer cupoMinimo, Integer cupoMaximo,
                  ComisionLectiva comisionLectiva) {
        this.nombre = nombre;
        this.cupoMinimo = cupoMinimo;
        this.cupoMaximo = cupoMaximo;
        this.comisionLectiva = comisionLectiva;
        this.estado = EstadoEquipo.ABIERTO;
    }

    public Equipo(Long id, String nombre, Integer cupoMinimo, Integer cupoMaximo,
                  EstadoEquipo estado, ComisionLectiva comisionLectiva, Estudiante representante) {
        this.id = id;
        this.nombre = nombre;
        this.cupoMinimo = cupoMinimo;
        this.cupoMaximo = cupoMaximo;
        this.estado = estado;
        this.comisionLectiva = comisionLectiva;
        this.representante = representante;
    }

    /**
     * Delegación al comportamiento del estado. El estado decide si la operación
     * es válida; el service conserva las condiciones de dominio.
     */
    public void ingresar() {
        estado.comportamiento().ingresar(this);
    }

    public void salir() {
        estado.comportamiento().salir(this);
    }

    public void asegurar() {
        estado.comportamiento().asegurar(this);
    }

    public void reabrir() {
        estado.comportamiento().reabrir(this);
    }

    public boolean isAsegurado() {
        return estado == EstadoEquipo.ASEGURADO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public ComisionLectiva getComisionLectiva() {
        return comisionLectiva;
    }

    public void setComisionLectiva(ComisionLectiva comisionLectiva) {
        this.comisionLectiva = comisionLectiva;
    }

    public Integer getCupoMinimo() {
        return cupoMinimo;
    }

    public void setCupoMinimo(Integer cupoMinimo) {
        this.cupoMinimo = cupoMinimo;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public Estudiante getRepresentante() {
        return representante;
    }

    public void setRepresentante(Estudiante representante) {
        this.representante = representante;
    }
}
