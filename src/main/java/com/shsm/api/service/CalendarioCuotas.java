package com.shsm.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CalendarioCuotas {

    private CalendarioCuotas() {}

    public static List<LocalDate> generar(LocalDate inicio, int meses, String frecuencia) {
        int cantidad = switch (frecuencia) {
            case "SEMANAL" -> meses * 4;
            case "QUINCENAL" -> meses * 2;
            case "ANUAL" -> (meses + 11) / 12;
            default -> meses;
        };

        List<LocalDate> fechas = new ArrayList<>(cantidad);
        for (int indice = 0; indice < cantidad; indice++) {
            LocalDate fecha = switch (frecuencia) {
                case "SEMANAL" -> inicio.plusWeeks(indice);
                case "QUINCENAL" -> inicio.plusDays(indice * 15L);
                case "ANUAL" -> inicio.plusYears(indice);
                default -> inicio.plusMonths(indice);
            };
            fechas.add(fecha);
        }
        return List.copyOf(fechas);
    }
}