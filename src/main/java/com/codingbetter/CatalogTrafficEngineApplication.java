package com.codingbetter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CatalogTrafficEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogTrafficEngineApplication.class, args);
    }
}

