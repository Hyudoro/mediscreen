package com.mediscreen.patient.service;

import com.mediscreen.patient.domain.Patient;
import com.mediscreen.patient.dto.PatientRequest;
import com.mediscreen.patient.dto.PatientResponse;
import com.mediscreen.patient.exception.PatientNotFoundException;
import com.mediscreen.patient.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Patient use cases. The boundary where entities stop and DTOs start. */
@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public Page<PatientResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(PatientResponse::from);
    }

    public PatientResponse findById(long id) {
        return repository.findById(id).map(PatientResponse::from).orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        var patient = new Patient(
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.gender(),
                request.street(),
                request.phoneNumber());
        return PatientResponse.from(repository.save(patient));
    }

    @Transactional
    public PatientResponse update(long id, PatientRequest request) {
        var patient = repository.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhoneNumber(request.phoneNumber());
        patient.setStreet(request.street());
        return PatientResponse.from(repository.save(patient));
    }
}
