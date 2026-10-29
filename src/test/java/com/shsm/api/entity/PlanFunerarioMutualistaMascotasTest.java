package com.shsm.api.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanFunerarioMutualistaMascotasTest {

    @Test
    void aplicaDefaultsAunqueElPlanTengaOtrosValores() {
        PlanFunerario plan = new PlanFunerario();
        plan.setAportacionDiaria(new BigDecimal("99"));
        plan.setPrecioTotal(new BigDecimal("9999"));
        plan.setMensualidad(new BigDecimal("999"));
        plan.setDuracionMeses((short) 24);
        plan.setNumeroBeneficiarios((short) 5);
        plan.setPorcentajeExtraMascota(new BigDecimal("15"));

        plan.aplicarMutualistaMascotas();

        assertThat(plan.getAportacionDiaria()).isEqualByComparingTo("1.00");
        assertThat(plan.getPrecioTotal()).isEqualByComparingTo("365.00");
        assertThat(plan.getMensualidad()).isEqualByComparingTo("30.42");
        assertThat(plan.getDuracionMeses()).isEqualTo((short) 12);
        assertThat(plan.getNumeroBeneficiarios()).isZero();
        assertThat(plan.getPorcentajeExtraMascota()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(plan.getCondiciones()).contains("precio total del plan", "aportaciones pendientes")
                .doesNotContain("365.00");
    }
}