package com.facturacion.cliente;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {

    private static final Locale SPAIN = Locale.of("es", "ES");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formats() {
    }

    // 1234.5 -> "1.234,50 €"
    public static String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(SPAIN);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " €";
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }
}