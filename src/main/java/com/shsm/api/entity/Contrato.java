package com.shsm.api.entity;

import com.shsm.api.entity.catalog.EstadoContrato;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contratos")
@Getter
@Setter
public class Contrato extends BaseEntity {

    @Column(name = "numero_contrato", nullable = false, unique = true, length = 30)
    private String numeroContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanFunerario plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_vendedor_id")
    private Empleado empleadoVendedor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoContrato estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "precio_contratado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioContratado;

    @Column(name = "mensualidad_pactada", nullable = false, precision = 10, scale = 2)
    private BigDecimal mensualidadPactada;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
