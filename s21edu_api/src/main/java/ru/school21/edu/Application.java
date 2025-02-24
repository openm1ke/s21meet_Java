package ru.school21.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EntityScan(basePackages = {"ru.school21.edu.generated.model", "ru.school21.edu.model"})
@ComponentScan(basePackages = {"edu", "ru.school21.edu"})
@EnableJpaRepositories(basePackages = {"edu.repository", "ru.school21.edu.repository"})
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
