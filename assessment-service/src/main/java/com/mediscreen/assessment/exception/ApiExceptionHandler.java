package com.mediscreen.assessment.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem details. The base class covers the errors Spring MVC raises
 * itself, such as an id that is not a number.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    public ProblemDetail handleNotFound(PatientNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Patient not found");
        problem.setType(URI.create("https://mediscreen.com/problems/patient-not-found"));
        problem.setProperty("patientId", exception.getPatientId());
        return problem;
    }

    /** The gateway, patient-service or note-service failed, or answered with an unexpected error. */
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleUnavailableData(RestClientException exception) {
        logger.warn("Could not read the patient's record or notes through the gateway", exception);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "The patient's record or notes could not be read");
        problem.setTitle("Patient data unavailable");
        problem.setType(URI.create("https://mediscreen.com/problems/patient-data-unavailable"));
        return problem;
    }
}
