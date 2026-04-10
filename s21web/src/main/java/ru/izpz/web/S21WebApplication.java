package ru.izpz.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.izpz.web.client")
public class S21WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(S21WebApplication.class, args);
    }
}
