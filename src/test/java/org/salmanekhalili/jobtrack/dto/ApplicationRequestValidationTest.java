package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The size limits are not decoration: they mirror the column widths in the
 * Flyway migrations, and a value that slips past them would come back as a 500
 * from Postgres instead of a 400 from the API.
 */
class ApplicationRequestValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        // ParameterMessageInterpolator keeps the test free of an EL provider.
        factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void acceptsTheMinimalHappyPathPayload() {
        Set<ConstraintViolation<ApplicationRequest>> violations =
                validator.validate(new ApplicationRequest("Example GmbH", "Backend Engineer", null, null, null, null));

        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsBlankCompanyAndRole() {
        Set<ConstraintViolation<ApplicationRequest>> violations =
                validator.validate(new ApplicationRequest(" ", "", ApplicationStatus.APPLIED, null, null, null));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("company", "role");
    }

    @Test
    void rejectsAJobUrlThatIsNotHttp() {
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(
                new ApplicationRequest("Acme", "Backend", ApplicationStatus.APPLIED, "javascript:alert(1)", null, null));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("jobUrl");
    }

    @Test
    void rejectsOptionalFieldsLongerThanTheirColumns() {
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(
                new ApplicationRequest("Acme", "Backend", ApplicationStatus.APPLIED,
                        "https://example.com/" + "x".repeat(300), "y".repeat(600), null));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("jobUrl", "salaryRange");
    }

    @Test
    void rejectsAnAppliedDateInTheFuture() {
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(
                new ApplicationRequest("Acme", "Backend", ApplicationStatus.APPLIED, null, null,
                        Instant.now().plusSeconds(86_400)));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("appliedAt");
    }

    @Test
    void acceptsAPastAppliedDate() {
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(
                new ApplicationRequest("Acme", "Backend", ApplicationStatus.APPLIED, null, null,
                        Instant.parse("2020-03-01T10:15:30Z")));

        assertThat(violations).isEmpty();
    }

    @Test
    void putRequiresCompanyRoleAndStatus() {
        Set<ConstraintViolation<ApplicationReplaceRequest>> violations =
                validator.validate(new ApplicationReplaceRequest("Acme", "Backend", null, null, null, null));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("status");
    }

    @Test
    void patchAcceptsAnEmptyBody() {
        Set<ConstraintViolation<ApplicationPatchRequest>> violations = validator.validate(
                new ApplicationPatchRequest(null, null, null, null, null, null));

        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsANoteBodyLongerThanItsColumn() {
        Set<ConstraintViolation<NoteRequest>> violations =
                validator.validate(new NoteRequest("z".repeat(256)));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("body");
    }
}
