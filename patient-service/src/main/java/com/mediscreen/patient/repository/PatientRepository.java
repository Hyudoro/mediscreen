package com.mediscreen.patient.repository;

import com.mediscreen.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** A page of patients, each loaded with its address in the same query rather than one query each. */
    @Override
    @EntityGraph(attributePaths = {"address"})
    Page<Patient> findAll(Pageable pageable);
}
