package com.shsm.api.service;

import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.MascotaContrato;
import com.shsm.api.entity.Persona;
import com.shsm.api.entity.PlanFunerario;

import java.math.BigDecimal;
import java.time.LocalDate;

final class MutualistaMascotasFixtures {
    private MutualistaMascotasFixtures() {}

    static PlanFunerario plan() {
        PlanFunerario plan = new PlanFunerario();
        plan.setId(2L);
        plan.setNombre("Mutualista de Mascotas");
        plan.setTipoPlan("MUTUALISTA_MASCOTAS");
        plan.aplicarMutualistaMascotas();
        return plan;
    }

    static MascotaContrato mascota() {
        MascotaContrato mascota = new MascotaContrato();
        mascota.setNombre("Luna");
        mascota.setEspecie("Canina");
        mascota.setRaza("Labrador");
        mascota.setPeso(new BigDecimal("18.250"));
        mascota.setColorCaracteristicas("Negra con mancha blanca");
        return mascota;
    }

    static Persona persona() {
        Persona persona = new Persona();
        persona.setId(1L);
        persona.setNombre("Ana");
        persona.setApPaterno("Perez");
        persona.setApMaterno("Lopez");
        persona.setCurp("PELA900101MVZRPN01");
        persona.setRfc("PELA900101ABC");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("7821234567");
        persona.setCorreo("ana@example.test");
        persona.setCalle("Calle Prueba 10");
        persona.setColonia("Centro");
        persona.setMunicipio("Poza Rica");
        persona.setEstado("Veracruz");
        persona.setCodigoPostal("93200");
        return persona;
    }

    static Contrato contrato() {
        Contrato contrato = new Contrato();
        PlanFunerario plan = plan();
        contrato.setId(10L);
        contrato.setNumeroContrato("SHSM-2024-00010");
        contrato.setPersona(persona());
        contrato.setPlan(plan);
        contrato.setMascota(mascota());
        contrato.setFechaInicio(LocalDate.of(2024, 2, 29));
        contrato.setFechaVencimiento(LocalDate.of(2025, 2, 28));
        contrato.setPrecioContratado(plan.getPrecioTotal());
        contrato.setMensualidadPactada(plan.getMensualidad());
        contrato.setAportacionDiaria(plan.getAportacionDiaria());
        contrato.setCondiciones(plan.getCondiciones());
        contrato.setFrecuenciaPago("MENSUAL");
        return contrato;
    }
}