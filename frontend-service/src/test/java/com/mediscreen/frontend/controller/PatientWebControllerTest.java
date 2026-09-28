package com.mediscreen.frontend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.mediscreen.frontend.client.NoteClient;
import com.mediscreen.frontend.client.PatientClient;
import com.mediscreen.frontend.config.WebSecurityConfig;
import com.mediscreen.frontend.dto.NotePage;
import com.mediscreen.frontend.dto.NoteRequest;
import com.mediscreen.frontend.dto.NoteView;
import com.mediscreen.frontend.dto.PatientPage;
import com.mediscreen.frontend.dto.PatientView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.HttpClientErrorException;

@WebMvcTest(PatientWebController.class)
@Import(WebSecurityConfig.class)
@DisplayName("The patient screens")
class PatientWebControllerTest {

    private static final PatientView TEST_NONE = new PatientView(
            1L, "Test", "TestNone", LocalDate.of(1966, 12, 31), "FEMALE", "1 Brookside St", "100-222-3333");

    private static final PatientView TEST_BORDERLINE =
            new PatientView(2L, "Test", "TestBorderline", LocalDate.of(1945, 6, 24), "MALE", null, null);

    private static final NotePage NO_NOTES = new NotePage(List.of(), 0, 20, 0, 0);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PatientClient client;

    @MockitoBean
    private NoteClient noteClient;

    @Nested
    @DisplayName("when nobody is signed in")
    class Anonymous {

        @Test
        @DisplayName("sends the visitor to the login page")
        void redirectsToLogin() {
            assertThat(mvc.get().uri("/patients")).hasStatus3xxRedirection();
        }
    }

    @Nested
    @DisplayName("when signed in")
    class SignedIn {

        @Test
        @DisplayName("lists the patients, with the gender in the words the form uses")
        void listsThePatients() {
            given(client.list(0, 20)).willReturn(new PatientPage(List.of(TEST_NONE), 0, 20, 1, 1));

            assertThat(mvc.perform(get("/patients").with(user("tester"))))
                    .hasStatusOk()
                    .bodyText()
                    .contains("TestNone")
                    .contains("Female")
                    .doesNotContain("FEMALE");
        }

        @Test
        @DisplayName("shows one patient, marking an absent address rather than hiding it")
        void showsOnePatient() {
            given(client.get(2L)).willReturn(TEST_BORDERLINE);
            given(noteClient.list(2L, 0, 20)).willReturn(NO_NOTES);

            assertThat(mvc.perform(get("/patients/2").with(user("tester"))))
                    .hasStatusOk()
                    .bodyText()
                    .contains("TestBorderline")
                    .contains("Male")
                    .doesNotContain("MALE")
                    .contains("Not recorded")
                    .contains("No notes yet.");
        }

        @Test
        @DisplayName("answers a 404 with the not-found page when the patient does not exist")
        void answers404ForAnUnknownPatient() {
            given(client.get(99L))
                    .willThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), new byte[0], null));

            assertThat(mvc.perform(get("/patients/99").with(user("tester"))))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyText()
                    .contains("There is no patient or page at this address.");
        }

        @Test
        @DisplayName("accepts a new patient with no address and no phone number")
        void acceptsOptionalFieldsMissing() {
            given(client.create(any())).willReturn(TEST_NONE);

            assertThat(mvc.perform(post("/patients")
                            .param("firstName", "Jane")
                            .param("lastName", "Doe")
                            .param("birthDate", "1990-01-01")
                            .param("gender", "FEMALE")
                            .param("street", "")
                            .param("phoneNumber", "")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.FOUND);
        }

        @Test
        @DisplayName("redisplays the form when a required field is missing")
        void redisplaysTheForm() {
            assertThat(mvc.perform(post("/patients")
                            .param("firstName", "")
                            .param("lastName", "Doe")
                            .param("birthDate", "1990-01-01")
                            .param("gender", "FEMALE")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatusOk()
                    .bodyText()
                    .contains("Add a patient");
        }
    }

    @Nested
    @DisplayName("the notes on the patient page")
    class Notes {

        @Test
        @DisplayName("shows each note with its line breaks, escaped rather than rendered as HTML")
        void showsTheNotesAsTyped() {
            given(client.get(2L)).willReturn(TEST_BORDERLINE);
            given(noteClient.list(2L, 0, 20))
                    .willReturn(new NotePage(
                            List.of(new NoteView(
                                    "n1",
                                    2L,
                                    "TestBorderline",
                                    "Première ligne\nDeuxième <b>ligne</b>",
                                    Instant.parse("2026-09-01T09:00:00Z"))),
                            0,
                            20,
                            1,
                            1));

            assertThat(mvc.perform(get("/patients/2").with(user("tester"))))
                    .hasStatusOk()
                    .bodyText()
                    .contains("Première ligne\nDeuxième &lt;b&gt;ligne&lt;/b&gt;")
                    .doesNotContain("<b>ligne</b>");
        }

        @Test
        @DisplayName("adds a note exactly as typed and returns to the patient page")
        void addsANote() {
            given(client.get(2L)).willReturn(TEST_BORDERLINE);
            var text = "Première ligne\r\n  Deuxième ligne  ";

            assertThat(mvc.perform(post("/patients/2/notes")
                            .param("note", text)
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl("/patients/2");

            then(noteClient).should().add(new NoteRequest(2L, "TestBorderline", text));
        }

        @Test
        @DisplayName("redisplays the patient page when the note is blank")
        void refusesABlankNote() {
            given(client.get(2L)).willReturn(TEST_BORDERLINE);
            given(noteClient.list(2L, 0, 20)).willReturn(NO_NOTES);

            assertThat(mvc.perform(post("/patients/2/notes")
                            .param("note", "   ")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatusOk()
                    .bodyText()
                    .contains("TestBorderline")
                    .contains("must not be blank");

            then(noteClient).should(never()).add(any());
        }
    }
}
