package org.salmanekhalili.jobtrack.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.salmanekhalili.jobtrack.dto.ApiError;
import org.salmanekhalili.jobtrack.exception.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into the documented {@link ApiError} shape.
 *
 * <p>Anything not matched by a specific handler is a bug: it is logged with its
 * stack trace and answered with a generic 500 so nothing internal leaks to the
 * client.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return respond(ex.getStatus(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(),
                    fieldError.getDefaultMessage() == null ? "is invalid" : fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /** Unreadable body: malformed JSON, a wrong type, an unknown enum value. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        log.debug("Unreadable request body for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "No endpoint for " + request.getMethod() + " "
                + request.getRequestURI(), request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                 HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                 HttpServletRequest request) {
        return respond(HttpStatus.NOT_ACCEPTABLE, ex.getMessage(), request, Map.of());
    }

    /**
     * ResponseStatusException and the method-argument validation failure it
     * also covers: the framework has already decided these are client errors,
     * so its own wording is passed through.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiError> handleFramework(ErrorResponseException ex, HttpServletRequest request) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String message = ex.getBody() == null || ex.getBody().getDetail() == null
                ? statusCode.toString()
                : ex.getBody().getDetail();
        return respond(statusCode, message, request, Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' has an invalid value", request,
                Map.of());
    }

    /**
     * An unknown {@code sort=} field reaches the data layer and would otherwise
     * surface as a 500; it is a client mistake, so it is a 400.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleBadSort(PropertyReferenceException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST,
                "Cannot sort on '" + ex.getPropertyName() + "'. Sortable fields: id, status, company, role, "
                        + "appliedAt, updatedAt", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        // The only unique constraint in the schema is users.email, so this is
        // the unique-index violation winning the race against the pre-check in
        // AuthService rather than a genuine server fault.
        log.debug("Constraint violation on {}", request.getRequestURI(), ex);
        return respond(HttpStatus.CONFLICT, "Request conflicts with existing data", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, Map.of());
    }

    private ResponseEntity<ApiError> respond(HttpStatusCode status, String message,
                                             HttpServletRequest request, Map<String, String> fieldErrors) {
        int value = status.value();
        String reason = HttpStatus.resolve(value) == null ? status.toString() : HttpStatus.resolve(value).getReasonPhrase();
        ApiError body = new ApiError(Instant.now(), value, reason, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
