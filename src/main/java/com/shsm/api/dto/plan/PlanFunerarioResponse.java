package com.shsm.api.dto.plan;

import com.shsm.api.entity.PlanFunerario;

import java.math.BigDecimal;
import java.util.List;

public record PlanFunerarioResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipoPlan,
        BigDecimal precioTotal,
        BigDecimal mensualidad,
        Short duracionMeses,
        Short numeroBeneficiarios,
        BigDecimal porcentajeExtraMascota,
        Boolean activo,
        List<ArticuloIncluido> articulosIncluidos
) {
    public static PlanFunerarioResponse from(PlanFunerario plan) {
        List<ArticuloIncluido> incluidos = plan.getArticulos().stream()
                .map(pa -> new ArticuloIncluido(
                        pa.getArticulo().getNombre(),
                        pa.getArticulo().getDescripcion(),
                        pa.getModalidad()))
                .toList();
        return new PlanFunerarioResponse(
                plan.getId(), plan.getNombre(), plan.getDescripcion(), plan.getTipoPlan(),
                plan.getPrecioTotal(), plan.getMensualidad(), plan.getDuracionMeses(),
                plan.getNumeroBeneficiarios(), plan.getPorcentajeExtraMascota(), plan.getActivo(), incluidos);
    }

    public record ArticuloIncluido(String nombre, String descripcion, String modalidad) {}
}