package com.technovald;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TechNovaLdApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechNovaLdApplication.class, args);
    }

}
