package ru.izpz.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.izpz.bot.client")
public class S21BotApplication {
	public static void main(String[] args) {
		SpringApplication.run(S21BotApplication.class, args);
	}
}
