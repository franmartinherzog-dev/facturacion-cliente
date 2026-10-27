package com.facturacion.cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class InvoiceCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record Totals(BigDecimal taxableBase, BigDecimal vatAmount,
                         BigDecimal irpfAmount, BigDecimal total) {
    }

    private InvoiceCalculator() {
    }

    public static Totals calculate(List<ApiClient.InvoiceLineInput> lines,
                                   BigDecimal vatRate, BigDecimal irpfRate) {
        BigDecimal taxableBase = lines.stream()
                .map(line -> line.quantity().multiply(line.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = percentage(taxableBase, vatRate);
        BigDecimal irpfAmount = percentage(taxableBase, irpfRate);
        return new Totals(taxableBase, vatAmount, irpfAmount,
                taxableBase.add(vatAmount).subtract(irpfAmount));
    }

    private static BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}