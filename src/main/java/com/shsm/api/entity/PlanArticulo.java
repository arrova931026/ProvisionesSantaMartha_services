package com.shsm.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plan_articulos")
@Getter
@Setter
public class PlanArticulo {

    @EmbeddedId
    private PlanArticuloId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("planId")
    @JoinColumn(name = "plan_id")
    private PlanFunerario plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articuloId")
    @JoinColumn(name = "articulo_id")
    private CatalogoArticulo articulo;

    @Column(name = "cantidad", nullable = false)
    private Short cantidad = 1;
}
