package com.shsm.api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarioCuotasTest {

    private final LocalDate inicio = LocalDate.of(2026, 1, 31);

    @Test
    void generatesFourWeeklyAndTwoFortnightlyContributionsPerMonth() {
        var weekly = CalendarioCuotas.generar(inicio, 12, "SEMANAL");
        var fortnightly = CalendarioCuotas.generar(inicio, 12, "QUINCENAL");

        assertEquals(48, weekly.size());
        assertEquals(inicio.plusWeeks(1), weekly.get(1));
        assertEquals(24, fortnightly.size());
        assertEquals(inicio.plusDays(15), fortnightly.get(1));
    }

    @Test
    void generatesMonthlyAndAnnualContributions() {
        var monthly = CalendarioCuotas.generar(inicio, 12, "MENSUAL");
        var annual = CalendarioCuotas.generar(inicio, 24, "ANUAL");

        assertEquals(12, monthly.size());
        assertEquals(inicio.plusMonths(1), monthly.get(1));
        assertEquals(2, annual.size());
        assertTrue(annual.get(1).isEqual(inicio.plusYears(1)));
    }
}