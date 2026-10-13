package com.eventtracker.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EventValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validEventHasNoViolations() {
        Event event = new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", "Annual fest");

        assertThat(validator.validate(event)).isEmpty();
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        Event event = new Event(null, null, null, null);

        assertThat(invalidFields(validator.validate(event)))
                .containsExactlyInAnyOrder("name", "eventDate", "venue");
    }

    @Test
    void blankTextFieldsAreRejected() {
        Event event = new Event("   ", LocalDate.of(2026, 11, 5), "", null);

        assertThat(invalidFields(validator.validate(event)))
                .containsExactlyInAnyOrder("name", "venue");
    }

    @Test
    void overlongFieldsAreRejected() {
        Event event = new Event("a".repeat(151), LocalDate.of(2026, 11, 5), "v".repeat(151), "d".repeat(501));

        assertThat(invalidFields(validator.validate(event)))
                .containsExactlyInAnyOrder("name", "venue", "description");
    }

    private static Set<String> invalidFields(Set<ConstraintViolation<Event>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
