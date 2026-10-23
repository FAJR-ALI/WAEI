package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingProgramRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void acceptsSupportedDeliveryModes() {
        assertTrue(validator.validate(requestWithMode("IN_PERSON")).isEmpty());
        assertTrue(validator.validate(requestWithMode("ONLINE")).isEmpty());
    }

    @Test
    void rejectsHybridDeliveryMode() {
        assertTrue(validator.validate(requestWithMode("HYBRID")).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("deliveryMode")));
    }

    @Test
    void requiresDurationFields() {
        TrainingProgramRequest request = requestWithMode("IN_PERSON");
        request.setHoursPerDay(null);
        request.setMaxDays(null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("hoursPerDay")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("maxDays")));
    }

    @Test
    void rejectsDurationOutsideSupportedRange() {
        TrainingProgramRequest request = requestWithMode("IN_PERSON");
        request.setHoursPerDay(new BigDecimal("24.5"));
        request.setMaxDays(366);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("hoursPerDay")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("maxDays")));
    }

    private TrainingProgramRequest requestWithMode(String deliveryMode) {
        return TrainingProgramRequest.builder()
                .title("Awareness course")
                .category("صحة")
                .description("A course about awareness.")
                .price(BigDecimal.ONE)
                .hoursPerDay(BigDecimal.valueOf(2))
                .maxDays(5)
                .deliveryMode(deliveryMode)
                .build();
    }
}
