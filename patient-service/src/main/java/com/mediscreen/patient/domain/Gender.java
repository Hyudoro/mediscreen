package com.mediscreen.patient.domain;

import java.util.Arrays;

/**
 * Patient gender, stored as the single character the client's data sheet uses.
 *
 * <p>The database column is {@code CHAR(1)} constrained to {@code F} or {@code M}. {@link
 * GenderConverter} maps between the two, so the raw code never leaves the persistence layer.
 */
public enum Gender {
    FEMALE('F'),
    MALE('M');

    private final char code;

    Gender(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static Gender fromCode(char code) {
        return Arrays.stream(values())
                .filter(gender -> gender.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown gender code: " + code));
    }
}
