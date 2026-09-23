package com.mediscreen.frontend.controller;

import com.mediscreen.frontend.client.PatientClient;
import com.mediscreen.frontend.dto.PatientForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The patient screens: list, detail, add and update. */
@Controller
public class PatientWebController {

    private static final int PAGE_SIZE = 20;

    private final PatientClient client;

    public PatientWebController(PatientClient client) {
        this.client = client;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/patients";
    }

    @GetMapping("/patients")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("patients", client.list(page, PAGE_SIZE));
        return "patients/list";
    }

    @GetMapping("/patients/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("patient", client.get(id));
        return "patients/detail";
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
        var created = client.create(patientForm);
        return "redirect:/patients/" + created.id();
    }

    @GetMapping("/patients/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("patientForm", PatientForm.from(client.get(id)));
        model.addAttribute("patientId", id);
        return "patients/form";
    }

    @PostMapping("/patients/{id}")
    public String edit(@PathVariable long id, @Valid PatientForm patientForm, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("patientId", id);
            return "patients/form";
        }
        client.update(id, patientForm);
        return "redirect:/patients/" + id;
    }
}
