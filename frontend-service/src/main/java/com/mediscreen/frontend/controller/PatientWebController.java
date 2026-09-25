package com.mediscreen.frontend.controller;

import com.mediscreen.frontend.client.NoteClient;
import com.mediscreen.frontend.client.PatientClient;
import com.mediscreen.frontend.dto.PatientForm;
import com.mediscreen.frontend.dto.PatientView;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The patient screens: list, detail with the patient's notes, add and update. */
@Controller
public class PatientWebController {

    private static final int PAGE_SIZE = 20;

    private final PatientClient patientClient;
    private final NoteClient noteClient;

    public PatientWebController(PatientClient patientClient, NoteClient noteClient) {
        this.patientClient = patientClient;
        this.noteClient = noteClient;
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
        return detailPage(patientClient.get(id), page, model);
    }

    @GetMapping("/patients/new")
    public String addForm(Model model) {
        model.addAttribute("patientForm", new PatientForm());
        model.addAttribute("patientId", null);
        return "patients/form";
    }

    @PostMapping("/patients")
    public String add(@Valid PatientForm patientForm, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("patientId", null);
            return "patients/form";
        }
        var created = patientClient.create(patientForm);
        return "redirect:/patients/" + created.id();
    }

    @GetMapping("/patients/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("patientForm", PatientForm.from(patientClient.get(id)));
        model.addAttribute("patientId", id);
        return "patients/form";
    }

    @PostMapping("/patients/{id}")
    public String edit(@PathVariable long id, @Valid PatientForm patientForm, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("patientId", id);
            return "patients/form";
        }
        patientClient.update(id, patientForm);
        return "redirect:/patients/" + id;
    }

    private String detailPage(PatientView patient, int notesPage, Model model) {
        model.addAttribute("patient", patient);
        model.addAttribute("notes", noteClient.list(patient.id(), notesPage, PAGE_SIZE));
        return "patients/detail";
    }
}
