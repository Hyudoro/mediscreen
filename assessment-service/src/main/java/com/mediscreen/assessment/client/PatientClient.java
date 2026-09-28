package com.mediscreen.assessment.client;

import com.mediscreen.assessment.dto.Patient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** The patient endpoint the assessment reads, through the gateway. */
@HttpExchange("/api/v1/patients")
public interface PatientClient {

    @GetExchange("/{id}")
    Patient get(@PathVariable long id);
}
