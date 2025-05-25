package ru.izpz.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "ru.izpz.edu.model")
@EnableJpaRepositories(basePackages = "ru.izpz.edu.repository")
@EnableScheduling
@EnableRetry
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class S21EduApplication {

    public static void main(String[] args) {
        SpringApplication.run(S21EduApplication.class, args);
    }
}
