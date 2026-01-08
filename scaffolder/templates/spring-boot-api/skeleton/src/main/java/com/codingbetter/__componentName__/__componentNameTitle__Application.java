package com.codingbetter.${{ parameters.componentName }};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ${{ parameters.componentName | title }}Application {

    public static void main(String[] args) {
        SpringApplication.run(${{ parameters.componentName | title }}Application.class, args);
    }
}

