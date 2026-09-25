package com.mediscreen.frontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * The add and update form.
 *
 * <p>A class rather than a record because Thymeleaf form binding needs setters for an empty form
 * object. Street and phone number carry no {@code @NotBlank}: both are optional.
 */
public class PatientForm {

    @NotBlank
    @Size(max = 60)
    private String firstName;

    @NotBlank
    @Size(max = 60)
    private String lastName;

    @NotNull
    @Past
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotNull
    private String gender;

    @Size(max = 120)
    private String street;

    @Size(max = 20)
    private String phoneNumber;

    public static PatientForm from(PatientView patient) {
        var form = new PatientForm();
        form.firstName = patient.firstName();
        form.lastName = patient.lastName();
        form.birthDate = patient.birthDate();
        form.gender = patient.gender();
        form.street = patient.street();
        form.phoneNumber = patient.phoneNumber();
        return form;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
