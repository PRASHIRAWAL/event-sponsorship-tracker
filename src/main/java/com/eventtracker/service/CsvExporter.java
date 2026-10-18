package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.Sponsor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds CSV exports (RFC 4180 quoting). Values that a spreadsheet would treat as a formula
 * (starting with =, +, -, @, tab or carriage return) are prefixed with a quote so opening an export
 * can never run a formula that a user typed into a name or note.
 */
public final class CsvExporter {

    /** Phone numbers such as "+91 98765 43210" start with + but cannot form a formula, so they stay as typed. */
    private static final Pattern PLAIN_NUMBER = Pattern.compile("[+-][0-9 ().-]*");

    private CsvExporter() {
    }

    public static String sponsors(List<Sponsor> sponsors) {
        StringBuilder csv = new StringBuilder();
        row(csv, "ID", "Sponsor", "Industry", "Contact Person", "Email", "Phone", "Event", "Event Date", "Tier",
                "Status", "Amount", "Paid", "Outstanding", "Payment Status", "Payment Due", "Notes");
        for (Sponsor sponsor : sponsors) {
            Event event = sponsor.getEvent();
            row(csv,
                    String.valueOf(sponsor.getId()),
                    sponsor.getName(),
                    sponsor.getIndustry(),
                    sponsor.getContactPerson(),
                    sponsor.getEmail(),
                    sponsor.getPhone(),
                    event == null ? null : event.getName(),
                    event == null ? null : date(event.getEventDate()),
                    sponsor.getTier() == null ? null : sponsor.getTier().getLabel(),
                    sponsor.getSponsorshipStatus().getLabel(),
                    amount(sponsor.getSponsorshipAmount()),
                    amount(sponsor.getAmountPaid()),
                    amount(sponsor.getOutstandingAmount()),
                    sponsor.getPaymentStatus().getLabel(),
                    date(sponsor.getPaymentDueDate()),
                    sponsor.getNotes());
        }
        return csv.toString();
    }

    public static String events(List<EventFunding> fundings) {
        StringBuilder csv = new StringBuilder();
        row(csv, "ID", "Event", "Date", "Venue", "Status", "Target", "Committed", "Pipeline", "Collected",
                "Outstanding", "Percent of Target", "Active Sponsors");
        for (EventFunding funding : fundings) {
            Event event = funding.event();
            row(csv,
                    String.valueOf(event.getId()),
                    event.getName(),
                    date(event.getEventDate()),
                    event.getVenue(),
                    event.getStatus() == null ? null : event.getStatus().getLabel(),
                    amount(funding.target()),
                    amount(funding.committed()),
                    amount(funding.pipeline()),
                    amount(funding.collected()),
                    amount(funding.outstanding()),
                    funding.hasTarget() ? String.valueOf(funding.percentOfTarget()) : null,
                    String.valueOf(funding.sponsorCount()));
        }
        return csv.toString();
    }

    private static void row(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values[i]));
        }
        csv.append("\r\n");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0 && !PLAIN_NUMBER.matcher(safe).matches()) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    // Plain numbers (no thousands separator) so spreadsheets read them as numbers
    private static String amount(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String date(LocalDate value) {
        return value == null ? null : value.toString();
    }
}
