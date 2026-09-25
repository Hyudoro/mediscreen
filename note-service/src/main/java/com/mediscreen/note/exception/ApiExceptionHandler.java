package com.mediscreen.note.exception;

import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem details. The base class covers the errors Spring MVC raises
 * itself, such as a missing patientId parameter or a body that is not JSON.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
        problem.setTitle("Invalid note details");
        problem.setType(URI.create("https://mediscreen.com/problems/validation-failed"));
        problem.setProperty("errors", fieldErrors(exception));
        return handleExceptionInternal(exception, problem, headers, status, request);
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
