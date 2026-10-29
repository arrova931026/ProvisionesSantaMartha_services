package com.shsm.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
public class MascotaContrato {
    @NotBlank
    @Size(max = 150)
    @Column(name = "mascota_nombre", length = 150)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Column(name = "mascota_especie", length = 100)
    private String especie;

    @NotBlank
    @Size(max = 150)
    @Column(name = "mascota_raza", length = 150)
    private String raza;

    @NotNull
    @DecimalMin("0.001")
    @jakarta.validation.constraints.Digits(integer = 7, fraction = 3)
    @Column(name = "mascota_peso", precision = 10, scale = 3)
    private BigDecimal peso;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "mascota_color_caracteristicas", columnDefinition = "TEXT")
    private String colorCaracteristicas;
}