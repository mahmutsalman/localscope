package com.localscopelocal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for LocalScopeLocal
 */
@SpringBootApplication
@EnableScheduling
public class LocalScopeLocalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalScopeLocalApplication.class, args);
    }
} 