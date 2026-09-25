package com.mediscreen.frontend.client;

import com.mediscreen.frontend.dto.NotePage;
import com.mediscreen.frontend.dto.NoteRequest;
import com.mediscreen.frontend.dto.NoteView;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** The note endpoints as the front consumes them, through the gateway. */
@HttpExchange("/api/v1/notes")
public interface NoteClient {

    @GetExchange
    NotePage list(@RequestParam long patientId, @RequestParam int page, @RequestParam int size);

    @PostExchange
    NoteView add(@RequestBody NoteRequest note);
}
