package com.mediscreen.patient.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link Gender} to the {@code CHAR(1)} code held in the database, and back. */
@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, Character> {

    @Override
    public Character convertToDatabaseColumn(Gender gender) {
        return gender == null ? null : gender.code();
    }

    @Override
    public Gender convertToEntityAttribute(Character code) {
        return code == null ? null : Gender.fromCode(code);
    }
}
