package com.mediscreen.frontend.client;

import com.mediscreen.frontend.dto.AssessmentView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** The assessment endpoint as the front consumes it, through the gateway. */
@HttpExchange("/api/v1/assessments")
public interface AssessmentClient {

    @GetExchange("/{patientId}")
    AssessmentView get(@PathVariable long patientId);
}
