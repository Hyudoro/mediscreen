package com.mediscreen.patient.exception;

import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns application exceptions into RFC 9457 problem details. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    public ProblemDetail handleNotFound(PatientNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Patient not found");
        problem.setType(URI.create("https://mediscreen.com/problems/patient-not-found"));
        problem.setProperty("patientId", exception.getPatientId());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
        problem.setTitle("Invalid patient details");
        problem.setType(URI.create("https://mediscreen.com/problems/validation-failed"));
        problem.setProperty("errors", fieldErrors(exception));
        return problem;
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .forEach(error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage()));
        return errors;
    }
}
