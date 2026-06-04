package com.shsm.api.entity.catalog;

import com.shsm.api.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipos_documento")
@Getter
@Setter
public class TipoDocumento extends BaseEntity {

    @Column(name = "clave", nullable = false, unique = true, length = 50)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
