package com.shsm.api.entity;

import com.shsm.api.entity.catalog.Parentesco;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "beneficiarios")
@Getter
@Setter
public class Beneficiario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentesco_id")
    private Parentesco parentesco;

    @Column(name = "porcentaje_cobertura", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeCobertura = BigDecimal.valueOf(100.00);

    @Column(name = "es_titular", nullable = false)
    private Boolean esTitular = false;
}
