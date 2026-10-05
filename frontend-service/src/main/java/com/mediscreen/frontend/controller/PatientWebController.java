package com.mediscreen.frontend.controller;

import com.mediscreen.frontend.client.AssessmentClient;
import com.mediscreen.frontend.client.NoteClient;
import com.mediscreen.frontend.client.PatientClient;
import com.mediscreen.frontend.dto.NoteForm;
import com.mediscreen.frontend.dto.NoteRequest;
import com.mediscreen.frontend.dto.PatientForm;
import com.mediscreen.frontend.dto.PatientView;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

/** The patient screens: list, detail with the patient's notes and diabetes risk, add and update. */
@Controller
public class PatientWebController {

    private static final Logger LOG = LoggerFactory.getLogger(PatientWebController.class);

    private static final int PAGE_SIZE = 20;

    private static final String REDIRECT_TO_PATIENT = "redirect:/patients/";
    private static final String PATIENT_FORM_VIEW = "patients/form";
    private static final String PATIENT_ID_ATTRIBUTE = "patientId";

    private final PatientClient patientClient;
    private final NoteClient noteClient;
    private final AssessmentClient assessmentClient;

    public PatientWebController(PatientClient patientClient, NoteClient noteClient, AssessmentClient assessmentClient) {
        this.patientClient = patientClient;
        this.noteClient = noteClient;
        this.assessmentClient = assessmentClient;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/patients";
    }

    @GetMapping("/patients")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("patients", patientClient.list(page, PAGE_SIZE));
        return "patients/list";
    }

    @GetMapping("/patients/{id}")
    public String detail(@PathVariable long id, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("noteForm", new NoteForm());
        return detailPage(patientClient.get(id), page, model);
    }

    @PostMapping("/patients/{id}/notes")
    public String addNote(@PathVariable long id, @Valid NoteForm noteForm, BindingResult binding, Model model) {
        var patient = patientClient.get(id);
        if (binding.hasErrors()) {
            return detailPage(patient, 0, model);
        }
        noteClient.add(new NoteRequest(id, patient.lastName(), noteForm.getNote()));
        return REDIRECT_TO_PATIENT + id;
    }

    @GetMapping("/patients/new")
    public String addForm(Model model) {
        model.addAttribute("patientForm", new PatientForm());
        model.addAttribute(PATIENT_ID_ATTRIBUTE, null);
        return PATIENT_FORM_VIEW;
    }

    @PostMapping("/patients")
    public String add(@Valid PatientForm patientForm, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute(PATIENT_ID_ATTRIBUTE, null);
            return PATIENT_FORM_VIEW;
        }
        var created = patientClient.create(patientForm);
        return REDIRECT_TO_PATIENT + created.id();
    }

    @GetMapping("/patients/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("patientForm", PatientForm.from(patientClient.get(id)));
        model.addAttribute(PATIENT_ID_ATTRIBUTE, id);
        return PATIENT_FORM_VIEW;
    }

    @PostMapping("/patients/{id}")
    public String edit(@PathVariable long id, @Valid PatientForm patientForm, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute(PATIENT_ID_ATTRIBUTE, id);
            return PATIENT_FORM_VIEW;
        }
        patientClient.update(id, patientForm);
        return REDIRECT_TO_PATIENT + id;
    }

    private String detailPage(PatientView patient, int notesPage, Model model) {
        model.addAttribute("patient", patient);
        model.addAttribute("notes", noteClient.list(patient.id(), notesPage, PAGE_SIZE));
        model.addAttribute("riskLevel", riskLevel(patient.id()));
        return "patients/detail";
    }

    /** The risk level in words, or "Not available" when the assessment fails, so the rest of the page still shows. */
    private String riskLevel(long patientId) {
        try {
            return assessmentClient.get(patientId).riskLabel();
        } catch (RestClientException exception) {
            LOG.warn("No risk level for patient {}", patientId, exception);
            return "Not available";
        }
    }
}
