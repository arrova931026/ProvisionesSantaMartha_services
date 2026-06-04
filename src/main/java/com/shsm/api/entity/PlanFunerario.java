package com.shsm.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planes_funerarios")
@Getter
@Setter
public class PlanFunerario extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "mensualidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal mensualidad;

    @Column(name = "duracion_meses", nullable = false)
    private Short duracionMeses;

    @Column(name = "numero_beneficiarios", nullable = false)
    private Short numeroBeneficiarios = 1;

    @JsonIgnore
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanArticulo> articulos = new ArrayList<>();
}
