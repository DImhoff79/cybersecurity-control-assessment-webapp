package com.cyberassessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CybersecurityAssessmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CybersecurityAssessmentApplication.class, args);
    }
}
