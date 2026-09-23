package com.mediscreen.patient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.mediscreen.patient.domain.Gender;
import com.mediscreen.patient.domain.Patient;
import com.mediscreen.patient.dto.PatientRequest;
import com.mediscreen.patient.exception.PatientNotFoundException;
import com.mediscreen.patient.repository.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("The patient service")
class PatientServiceTest {

    @Mock
    private PatientRepository repository;

    @InjectMocks
    private PatientService service;

    private static Patient testNone() {
        return new Patient(
                "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "1 Brookside St", "100-222-3333");
    }

    @Nested
    @DisplayName("when listing patients")
    class Listing {

        @Test
        @DisplayName("returns a page of responses rather than entities")
        void returnsAPageOfResponses() {
            given(repository.findAll(any(PageRequest.class))).willReturn(new PageImpl<>(List.of(testNone())));

            var page = service.findAll(PageRequest.of(0, 20));

            assertThat(page.getContent()).singleElement().satisfies(response -> {
                assertThat(response.lastName()).isEqualTo("TestNone");
                assertThat(response.street()).isEqualTo("1 Brookside St");
            });
        }
    }

    @Nested
    @DisplayName("when fetching one patient")
    class Fetching {

        @Test
        @DisplayName("returns the patient when the id is known")
        void returnsThePatient() {
            given(repository.findById(1L)).willReturn(Optional.of(testNone()));

            var response = service.findById(1L);

            assertThat(response.lastName()).isEqualTo("TestNone");
            assertThat(response.gender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("fails when the id is unknown")
        void failsOnUnknownId() {
            given(repository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("when adding a patient")
    class Adding {

        @Test
        @DisplayName("accepts a record with no address and no phone number")
        void acceptsARecordWithoutOptionalFields() {
            given(repository.save(any(Patient.class))).willAnswer(call -> call.getArgument(0));

            var response = service.create(
                    new PatientRequest("Jane", "Doe", LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null));

            assertThat(response.street()).isNull();
            assertThat(response.phoneNumber()).isNull();

            var saved = ArgumentCaptor.forClass(Patient.class);
            then(repository).should().save(saved.capture());
            assertThat(saved.getValue().getStreet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("when updating a patient")
    class Updating {

        @Test
        @DisplayName("overwrites the stored details")
        void overwritesTheDetails() {
            given(repository.findById(1L)).willReturn(Optional.of(testNone()));
            given(repository.save(any(Patient.class))).willAnswer(call -> call.getArgument(0));

            var response = service.update(
                    1L,
                    new PatientRequest(
                            "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "9 New Road", "111"));

            assertThat(response.street()).isEqualTo("9 New Road");
            assertThat(response.phoneNumber()).isEqualTo("111");
        }

        @Test
        @DisplayName("clears the address when the street is left blank")
        void clearsTheAddress() {
            given(repository.findById(1L)).willReturn(Optional.of(testNone()));
            given(repository.save(any(Patient.class))).willAnswer(call -> call.getArgument(0));

            var response = service.update(
                    1L, new PatientRequest("Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "  ", null));

            assertThat(response.street()).isNull();
        }

        @Test
        @DisplayName("fails and saves nothing when the id is unknown")
        void failsOnUnknownId() {
            given(repository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(
                            99L,
                            new PatientRequest("Jane", "Doe", LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null)))
                    .isInstanceOf(PatientNotFoundException.class);

            then(repository).should(never()).save(any());
        }
    }
}
