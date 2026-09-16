package com.mediscreen.patient;

import org.springframework.boot.SpringApplication;

public class TestPatientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PatientServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
