package com.mediscreen.patient.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A patient record.
 *
 * <p>Address and phone number are optional for a valid record, as the sprint 1 user story requires.
 * The address is modelled as a separate entity rather than embedded columns; see {@link Address}.
 */
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "char(1)")
    private Gender gender;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Address address;

    protected Patient() {
        // for JPA
    }

    public Patient(
            String firstName, String lastName, LocalDate birthDate, Gender gender, String street, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        setStreet(street);
    }

    /**
     * Sets, replaces or clears the postal address.
     *
     * <p>A blank street removes the row entirely rather than storing an empty string, which keeps
     * "no address" a single representable state.
     */
    public void setStreet(String street) {
        if (street == null || street.isBlank()) {
            this.address = null;
            return;
        }
        if (this.address == null) {
            this.address = new Address(this, street);
        } else {
            this.address.setStreet(street);
        }
    }

    public Long getId() {
        return id;
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

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Optional<String> getStreet() {
        return Optional.ofNullable(address).map(Address::getStreet);
    }
}
