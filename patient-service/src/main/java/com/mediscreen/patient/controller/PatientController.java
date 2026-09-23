package com.mediscreen.patient.controller;

import com.mediscreen.patient.dto.PatientRequest;
import com.mediscreen.patient.dto.PatientResponse;
import com.mediscreen.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Personal information of the clinic's patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List patients, one page at a time")
    public Page<PatientResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one patient's personal information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The patient was found"),
        @ApiResponse(responseCode = "404", description = "No patient with that id")
    })
    public PatientResponse get(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Add a patient")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The patient was created"),
        @ApiResponse(responseCode = "400", description = "The submitted details are invalid")
    })
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/patients/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a patient's personal information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The patient was updated"),
        @ApiResponse(responseCode = "400", description = "The submitted details are invalid"),
        @ApiResponse(responseCode = "404", description = "No patient with that id")
    })
    public PatientResponse update(@PathVariable long id, @Valid @RequestBody PatientRequest request) {
        return service.update(id, request);
    }
}
