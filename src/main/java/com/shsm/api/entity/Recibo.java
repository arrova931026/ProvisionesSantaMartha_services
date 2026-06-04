package com.shsm.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recibos")
@Getter
@Setter
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(name = "folio_recibo", nullable = false, unique = true, length = 50)
    private String folioRecibo;

    @Column(name = "ruta_pdf", columnDefinition = "TEXT")
    private String rutaPdf;

    @Column(name = "enviado_correo", nullable = false)
    private Boolean enviadoCorreo = false;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (fechaEmision == null) fechaEmision = OffsetDateTime.now();
    }
}
