package com.mediscreen.note.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.mediscreen.note.dto.NoteRequest;
import com.mediscreen.note.dto.NoteResponse;
import com.mediscreen.note.service.NoteService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(NoteController.class)
@DisplayName("The note endpoint")
class NoteControllerTest {

    private static final NoteResponse TEST_NONE_NOTE = new NoteResponse(
            "n1",
            1L,
            "TestNone",
            "Le patient déclare qu'il 'se sent très bien' Poids égal ou inférieur au poids recommandé",
            Instant.parse("2026-09-01T09:00:00Z"));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private NoteService service;

    @Nested
    @DisplayName("without credentials")
    class Unauthenticated {

        @Test
        @DisplayName("answers 401 rather than serving notes")
        void refusesToList() {
            assertThat(mvc.get().uri("/api/v1/notes?patientId=1")).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("with credentials")
    class Authenticated {

        @Test
        @DisplayName("lists a patient's notes, newest first unless asked otherwise")
        void listsNewestFirst() {
            given(service.findByPatientId(eq(1L), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(TEST_NONE_NOTE)));

            assertThat(mvc.perform(get("/api/v1/notes").param("patientId", "1").with(user("tester"))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content[0].note")
                    .isEqualTo(TEST_NONE_NOTE.note());

            var pageable = ArgumentCaptor.forClass(Pageable.class);
            then(service).should().findByPatientId(eq(1L), pageable.capture());
            assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
                    .isNotNull()
                    .extracting(Sort.Order::getDirection)
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when patientId is missing")
        void answers400WithoutPatientId() {
            assertThat(mvc.perform(get("/api/v1/notes").with(user("tester"))))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.detail")
                    .asString()
                    .contains("patientId");
        }

        @Test
        @DisplayName("adds a note and answers 201")
        void addsANote() {
            given(service.add(any())).willReturn(TEST_NONE_NOTE);

            assertThat(mvc.perform(post("/api/v1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"patientId":1,"patient":"TestNone","note":"Poids égal ou inférieur"}
                                    """)
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .isEqualTo("n1");
        }

        @Test
        @DisplayName("passes the note on exactly as typed, line breaks included")
        void keepsTheTextAsTyped() {
            given(service.add(any())).willReturn(TEST_NONE_NOTE);

            assertThat(mvc.perform(post("/api/v1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"patientId":1,"patient":"TestNone","note":"  Première ligne\\nDeuxième ligne  \\r\\n"}
                                    """)
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.CREATED);

            var request = ArgumentCaptor.forClass(NoteRequest.class);
            then(service).should().add(request.capture());
            assertThat(request.getValue().note()).isEqualTo("  Première ligne\nDeuxième ligne  \r\n");
        }

        @Test
        @DisplayName("accepts a very long note, since notes have no size limit")
        void acceptsAVeryLongNote() {
            given(service.add(any())).willReturn(TEST_NONE_NOTE);
            var longNote = "Poids anormal. ".repeat(20_000);

            assertThat(mvc.perform(post("/api/v1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patientId\":1,\"patient\":\"TestNone\",\"note\":\"" + longNote + "\"}")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.CREATED);

            var request = ArgumentCaptor.forClass(NoteRequest.class);
            then(service).should().add(request.capture());
            assertThat(request.getValue().note()).isEqualTo(longNote);
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when the note is blank")
        void answers400WhenBlank() {
            assertThat(mvc.perform(post("/api/v1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"patientId":1,"patient":"TestNone","note":"   "}
                                    """)
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.errors.note")
                    .isNotNull();
            then(service).should(never()).add(any());
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when the body is not JSON")
        void answers400ForABodyThatIsNotJson() {
            assertThat(mvc.perform(post("/api/v1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("Poids anormal")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson()
                    .extractingPath("$.title")
                    .isEqualTo("Bad Request");
            then(service).should(never()).add(any());
        }
    }
}
