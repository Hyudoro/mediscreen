package com.mediscreen.note;

import org.springframework.boot.SpringApplication;

public class TestNoteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(NoteServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
