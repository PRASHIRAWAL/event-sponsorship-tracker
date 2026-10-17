package com.eventtracker.model;

public enum ActivityType {

    CREATED("Created"),
    UPDATED("Updated"),
    DELETED("Deleted"),
    PAYMENT_RECORDED("Payment recorded"),
    PAYMENT_REMOVED("Payment removed");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
