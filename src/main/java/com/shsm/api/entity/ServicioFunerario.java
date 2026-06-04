package com.shsm.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "servicios_funerarios")
@Getter
@Setter
public class ServicioFunerario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiario beneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_responsable")
    private Empleado empleadoResponsable;

    @Column(name = "fecha_fallecimiento", nullable = false)
    private LocalDate fechaFallecimiento;

    @Column(name = "fecha_servicio")
    private LocalDate fechaServicio;

    @Column(name = "lugar_servicio", columnDefinition = "TEXT")
    private String lugarServicio;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;
}
