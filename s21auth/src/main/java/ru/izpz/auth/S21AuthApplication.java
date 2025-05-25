package ru.izpz.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "ru.izpz.auth")
@EnableScheduling
public class S21AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(S21AuthApplication.class, args);
    }
}
