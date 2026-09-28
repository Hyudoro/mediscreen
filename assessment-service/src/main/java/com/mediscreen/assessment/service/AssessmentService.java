package com.mediscreen.assessment.service;

import com.mediscreen.assessment.client.NoteClient;
import com.mediscreen.assessment.client.PatientClient;
import com.mediscreen.assessment.domain.RiskFactors;
import com.mediscreen.assessment.domain.RiskLevel;
import com.mediscreen.assessment.dto.AssessmentResponse;
import com.mediscreen.assessment.dto.NotePage;
import com.mediscreen.assessment.dto.Patient;
import com.mediscreen.assessment.exception.PatientNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/** Assesses a patient's diabetes risk from their record and all their notes, read through the gateway. */
@Service
public class AssessmentService {

    /** Most patients have fewer notes than this, so their history takes a single request. */
    private static final int NOTES_PER_PAGE = 100;

    private final PatientClient patientClient;
    private final NoteClient noteClient;
    private final Clock clock;

    public AssessmentService(PatientClient patientClient, NoteClient noteClient, Clock clock) {
        this.patientClient = patientClient;
        this.noteClient = noteClient;
        this.clock = clock;
    }

    public AssessmentResponse assess(long patientId) {
        var patient = findPatient(patientId);
        var factors = RiskFactors.of(allNotes(patientId), patient.birthDate(), patient.gender(), LocalDate.now(clock));
        return AssessmentResponse.from(patientId, RiskLevel.of(factors));
    }

    private Patient findPatient(long patientId) {
        try {
            return patientClient.get(patientId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new PatientNotFoundException(patientId);
        }
    }

    /** Every page of the patient's notes: the rules count the terms across the whole history. */
    private List<String> allNotes(long patientId) {
        var notes = new ArrayList<String>();
        var pageNumber = 0;
        NotePage page;
        do {
            page = noteClient.list(patientId, pageNumber++, NOTES_PER_PAGE);
            page.content().forEach(note -> notes.add(note.note()));
        } while (pageNumber < page.totalPages());
        return notes;
    }
}
