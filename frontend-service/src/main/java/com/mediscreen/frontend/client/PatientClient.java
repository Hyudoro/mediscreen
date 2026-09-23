package com.mediscreen.frontend.client;

import com.mediscreen.frontend.dto.PatientForm;
import com.mediscreen.frontend.dto.PatientPage;
import com.mediscreen.frontend.dto.PatientView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/** The patient endpoints as the front consumes them, through the gateway. */
@HttpExchange("/api/v1/patients")
public interface PatientClient {

    @GetExchange
    PatientPage list(@RequestParam int page, @RequestParam int size);

    @GetExchange("/{id}")
    PatientView get(@PathVariable long id);

    @PostExchange
    PatientView create(@RequestBody PatientForm form);

    @PutExchange("/{id}")
    PatientView update(@PathVariable long id, @RequestBody PatientForm form);
}
