package com.shsm.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sucursales")
@Getter
@Setter
public class Sucursal extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "calle", length = 200)
    private String calle;

    @Column(name = "numero_ext", length = 20)
    private String numeroExt;

    @Column(name = "numero_int", length = 20)
    private String numeroInt;

    @Column(name = "colonia", length = 150)
    private String colonia;

    @Column(name = "municipio", length = 150)
    private String municipio;

    @Column(name = "estado", length = 100)
    private String estado;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "codigo_postal", columnDefinition = "CHAR(5)")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, length = 100)
    private String pais = "México";

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 254)
    private String correo;
}
