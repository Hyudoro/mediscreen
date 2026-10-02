package com.mediscreen.assessment.controller;

import com.mediscreen.assessment.dto.AssessmentResponse;
import com.mediscreen.assessment.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Assessments", description = "A patient's diabetes risk")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Assess a patient's diabetes risk from their age, gender and notes")
    @ApiResponse(responseCode = "200", description = "The patient's risk level")
    @ApiResponse(responseCode = "404", description = "No patient with that id")
    @ApiResponse(responseCode = "502", description = "The patient's record or notes could not be read")
    public AssessmentResponse assess(@PathVariable long patientId) {
        return service.assess(patientId);
    }
}
