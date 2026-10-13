package com.eventtracker.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SponsorFormValidationTest {

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
    void validFormHasNoViolations() {
        SponsorForm form = validForm();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void onlyNameAndStatusAreRequired() {
        SponsorForm form = new SponsorForm();
        form.setName("Acme Corp");
        form.setSponsorshipStatus(SponsorshipStatus.PROPOSED);
        form.setEmail("");
        form.setPhone("");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        SponsorForm form = new SponsorForm();
        form.setName("  ");

        assertThat(invalidFields(validator.validate(form)))
                .containsExactlyInAnyOrder("name", "sponsorshipStatus");
    }

    @Test
    void invalidEmailPhoneAndAmountAreRejected() {
        SponsorForm form = validForm();
        form.setEmail("not-an-email");
        form.setPhone("call me");
        form.setSponsorshipAmount(new BigDecimal("-5"));

        assertThat(invalidFields(validator.validate(form)))
                .containsExactlyInAnyOrder("email", "phone", "sponsorshipAmount");
    }

    @Test
    void zeroAmountAndTooManyDecimalsAreRejected() {
        SponsorForm zero = validForm();
        zero.setSponsorshipAmount(BigDecimal.ZERO);
        SponsorForm decimals = validForm();
        decimals.setSponsorshipAmount(new BigDecimal("10.123"));

        assertThat(invalidFields(validator.validate(zero))).containsExactly("sponsorshipAmount");
        assertThat(invalidFields(validator.validate(decimals))).containsExactly("sponsorshipAmount");
    }

    @Test
    void overlongFieldsAreRejected() {
        SponsorForm form = validForm();
        form.setName("n".repeat(151));
        form.setContactPerson("c".repeat(101));

        assertThat(invalidFields(validator.validate(form)))
                .containsExactlyInAnyOrder("name", "contactPerson");
    }

    private static SponsorForm validForm() {
        SponsorForm form = new SponsorForm();
        form.setName("Acme Corp");
        form.setContactPerson("Jane Doe");
        form.setEmail("jane@acme.com");
        form.setPhone("+91 98765-43210");
        form.setSponsorshipAmount(new BigDecimal("50000.00"));
        form.setSponsorshipStatus(SponsorshipStatus.CONFIRMED);
        return form;
    }

    private static Set<String> invalidFields(Set<ConstraintViolation<SponsorForm>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
