package com.shsm.api.service;

import com.shsm.api.controller.TarifaController;
import com.shsm.api.dto.plan.PlanTarifaRequest;
import com.shsm.api.entity.CatalogoArticulo;
import com.shsm.api.entity.PlanArticulo;
import com.shsm.api.entity.PlanFunerario;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.repository.CatalogoArticuloRepository;
import com.shsm.api.repository.PlanFunerarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifaMutualistaMascotasTest {
    @Mock PlanFunerarioRepository planes;
    @Mock CatalogoArticuloRepository articulos;
    @InjectMocks TarifaController controller;

    private PlanTarifaRequest request(List<Long> articuloIds) {
        return new PlanTarifaRequest("Mutualista de Mascotas", "Plan de prueba",
                new BigDecimal("9999.99"), new BigDecimal("999.99"),
                (short) 24, (short) 8, new BigDecimal("15.00"), articuloIds);
    }

    private void verificarDefaults(PlanFunerario plan) {
        assertThat(plan.getTipoPlan()).isEqualTo("MUTUALISTA_MASCOTAS");
        assertThat(plan.getAportacionDiaria()).isEqualByComparingTo("1.00");
        assertThat(plan.getPrecioTotal()).isEqualByComparingTo("365.00");
        assertThat(plan.getMensualidad()).isEqualByComparingTo("30.42");
        assertThat(plan.getDuracionMeses()).isEqualTo((short) 12);
        assertThat(plan.getNumeroBeneficiarios()).isZero();
        assertThat(plan.getPorcentajeExtraMascota()).isEqualByComparingTo("0");
        assertThat(plan.getArticulos()).isEmpty();
        assertThat(plan.getCondiciones()).contains("precio total del plan");
    }

    @Test
    void crearNormalizaTipoImponeDefaultsYDescartaArticulos() {
        CatalogoArticulo articulo = new CatalogoArticulo();
        articulo.setId(7L);
        when(articulos.findById(7L)).thenReturn(Optional.of(articulo));
        when(planes.save(any(PlanFunerario.class))).thenAnswer(invocation -> {
            PlanFunerario plan = invocation.getArgument(0);
            plan.setId(2L);
            return plan;
        });

        var response = controller.crearPlan(" mutualista_mascotas ", request(List.of(7L)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<PlanFunerario> captor = ArgumentCaptor.forClass(PlanFunerario.class);
        verify(planes).save(captor.capture());
        verificarDefaults(captor.getValue());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().precioTotal()).isEqualByComparingTo("365.00");
        assertThat(response.getBody().numeroBeneficiarios()).isZero();
    }

    @Test
    void actualizarPermitePrecioYDuracionPersonalizados() {
        PlanFunerario plan = MutualistaMascotasFixtures.plan();
        plan.getArticulos().add(new PlanArticulo());
        when(planes.findByIdWithArticles(2L)).thenReturn(Optional.of(plan));
        when(planes.save(plan)).thenReturn(plan);

        var response = controller.actualizarPlan(2L, request(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(planes).save(plan);
        assertThat(plan.getTipoPlan()).isEqualTo("MUTUALISTA_MASCOTAS");
        assertThat(plan.getAportacionDiaria()).isEqualByComparingTo("1.00");
        assertThat(plan.getPrecioTotal()).isEqualByComparingTo("9999.99");
        assertThat(plan.getMensualidad()).isEqualByComparingTo("416.67");
        assertThat(plan.getDuracionMeses()).isEqualTo((short) 24);
        assertThat(plan.getNumeroBeneficiarios()).isZero();
        assertThat(plan.getPorcentajeExtraMascota()).isEqualByComparingTo("0");
        assertThat(plan.getArticulos()).isEmpty();
        assertThat(plan.getCondiciones()).contains("precio total del plan");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensualidad()).isEqualByComparingTo("416.67");
        verifyNoInteractions(articulos);
    }

    @Test
    void noPermiteEditarElPlanMutualistaMascotasBasico() {
        PlanFunerario plan = MutualistaMascotasFixtures.plan();
        plan.setNombre("Mutualista de Mascotas Básico");
        when(planes.findByIdWithArticles(2L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> controller.actualizarPlan(2L, request(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("predeterminado");
        verify(planes, never()).save(any());
    }

    @Test
    void listarReconoceElNuevoTipoNormalizado() {
        when(planes.findByTipoPlanWithArticles("MUTUALISTA_MASCOTAS"))
                .thenReturn(List.of(MutualistaMascotasFixtures.plan()));

        var response = controller.listarPorTipo(" mutualista_mascotas ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().tipoPlan()).isEqualTo("MUTUALISTA_MASCOTAS");
        verify(planes).findByTipoPlanWithArticles("MUTUALISTA_MASCOTAS");
        verifyNoInteractions(articulos);
    }
}