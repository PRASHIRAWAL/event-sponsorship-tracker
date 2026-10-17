package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @Test
    void quotesValuesWithCommasQuotesAndNewLines() {
        assertThat(CsvExporter.escape("plain")).isEqualTo("plain");
        assertThat(CsvExporter.escape("Food, Drinks")).isEqualTo("\"Food, Drinks\"");
        assertThat(CsvExporter.escape("The \"Big\" Co")).isEqualTo("\"The \"\"Big\"\" Co\"");
        assertThat(CsvExporter.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvExporter.escape(null)).isEmpty();
    }

    @Test
    void neutralisesSpreadsheetFormulasButKeepsPhoneNumbers() {
        assertThat(CsvExporter.escape("=HYPERLINK(\"http://x\")")).startsWith("\"'=HYPERLINK");
        assertThat(CsvExporter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvExporter.escape("-2+3+cmd|' /C calc'!A0")).startsWith("'-2+3");
        assertThat(CsvExporter.escape("+91 98765 43210")).isEqualTo("+91 98765 43210");
    }

    @Test
    void sponsorExportHasHeaderAndOneRowPerSponsor() {
        Event event = new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Hall", null);
        Sponsor sponsor = new Sponsor("Acme, Inc.", SponsorshipStatus.CONFIRMED);
        sponsor.setId(7L);
        sponsor.setEvent(event);
        sponsor.setTier(SponsorshipTier.GOLD);
        sponsor.setSponsorshipAmount(new BigDecimal("1000"));
        sponsor.setAmountPaid(new BigDecimal("250.5"));

        String[] lines = CsvExporter.sponsors(List.of(sponsor)).split("\r\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("ID,Sponsor,Industry");
        assertThat(lines[1]).startsWith("7,\"Acme, Inc.\",,")
                .contains("Tech Fest,2099-11-05,Gold,Confirmed,1000.00,250.50,749.50,Partially paid");
    }
}
