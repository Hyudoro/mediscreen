package com.mediscreen.frontend;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class FrontendServiceApplication {

    /**
     * Stops the startup when one of the service's secret files is missing. Boot would otherwise
     * bind the unresolved placeholder as its literal text and carry on.
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(FrontendServiceApplication.class)
                .initializers(context ->
                        context.getEnvironment().setRequiredProperties("service-password", "ui-password-hash"))
                .run(args);
    }
}
