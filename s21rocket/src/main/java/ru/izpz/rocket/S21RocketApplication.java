package ru.izpz.rocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("ru.izpz.rocket.property")
public class S21RocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(S21RocketApplication.class, args);
    }
}
