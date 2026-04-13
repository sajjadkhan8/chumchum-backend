package com.chamcham.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ChamchamBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamchamBackendApplication.class, args);
    }
}

