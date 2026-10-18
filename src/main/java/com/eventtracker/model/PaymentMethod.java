package com.eventtracker.model;

public enum PaymentMethod {

    BANK_TRANSFER("Bank transfer"),
    UPI("UPI"),
    CHEQUE("Cheque"),
    CARD("Card"),
    CASH("Cash");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
