package com.mediscreen.note;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDbContainer() {
        // mongod refuses to start on Linux 6.19 to 7.0.13 while TCMalloc uses its per-CPU caches.
        // With rseq left to glibc it does not use them, on any kernel.
        return new MongoDBContainer(DockerImageName.parse("mongo:8.3"))
                .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
    }
}
