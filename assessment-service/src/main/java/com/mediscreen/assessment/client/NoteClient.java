package com.mediscreen.assessment.client;

import com.mediscreen.assessment.dto.NotePage;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** The note endpoint the assessment reads, through the gateway. */
@HttpExchange("/api/v1/notes")
public interface NoteClient {

    @GetExchange
    NotePage list(@RequestParam long patientId, @RequestParam int page, @RequestParam int size);
}
