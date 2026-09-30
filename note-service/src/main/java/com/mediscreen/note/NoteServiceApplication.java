package com.mediscreen.note;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class NoteServiceApplication {

    /**
     * Stops the startup when one of the service's secret files is missing. Boot would otherwise
     * bind the unresolved placeholder as its literal text and carry on.
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(NoteServiceApplication.class)
                .initializers(context ->
                        context.getEnvironment().setRequiredProperties("mongo-password", "service-password-hash"))
                .run(args);
    }
}
