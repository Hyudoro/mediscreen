package com.mediscreen.patient;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PatientServiceApplication {

    /**
     * Stops the startup when one of the service's secret files is missing. Boot would otherwise
     * bind the unresolved placeholder as its literal text and carry on.
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(PatientServiceApplication.class)
                .initializers(context ->
                        context.getEnvironment().setRequiredProperties("postgres-password", "service-password-hash"))
                .run(args);
    }
}
