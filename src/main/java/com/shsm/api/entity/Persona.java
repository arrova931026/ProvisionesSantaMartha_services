package com.shsm.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "personas")
@Getter
@Setter
public class Persona extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "ap_paterno", nullable = false, length = 100)
    private String apPaterno;

    @Column(name = "ap_materno", length = 100)
    private String apMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "sexo", columnDefinition = "CHAR(1)")
    private String sexo;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "curp", columnDefinition = "CHAR(18)", unique = true)
    private String curp;

    @Column(name = "rfc", length = 13, unique = true)
    private String rfc;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "telefono_alt", length = 20)
    private String telefonoAlt;

    @Column(name = "correo", length = 254, unique = true)
    private String correo;

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

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
