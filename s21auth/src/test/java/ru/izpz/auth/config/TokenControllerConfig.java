package ru.izpz.auth.config;

import ru.izpz.auth.repository.TokenRepository;
import ru.izpz.auth.service.TokenService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;


@ActiveProfiles("test")
@TestConfiguration
public class TokenControllerConfig {

    @Bean
    public TokenService tokenService() {
        // Задаём фиктивные значения для конструктора
        String defaultLogin = "dummyLogin";
        String defaultPassword = "dummyPassword";
        // Мокаем зависимости
        TokenRepository tokenRepository = Mockito.mock(TokenRepository.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        // Возвращаем spy, чтобы можно было переопределить отдельные методы
        return Mockito.spy(new TokenService(defaultLogin, defaultPassword, tokenRepository, restTemplate));
    }
}
