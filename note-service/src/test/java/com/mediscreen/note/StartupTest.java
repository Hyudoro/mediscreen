package com.mediscreen.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MissingRequiredPropertiesException;

@DisplayName("The service's startup")
class StartupTest {

    @Test
    @DisplayName("stops when its secret files are missing, and names them")
    void stopsWithoutItsSecretFiles() {
        assertThatExceptionOfType(MissingRequiredPropertiesException.class)
                .isThrownBy(() -> NoteServiceApplication.main(new String[0]))
                .satisfies(exception -> assertThat(exception.getMissingRequiredProperties())
                        .containsExactlyInAnyOrder("mongo-password", "service-password-hash"));
    }
}
