package edu.config;

import edu.repository.TokenRepository;
import edu.service.TokenService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;


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
        WebClient webClient = Mockito.mock(WebClient.class);
        // Возвращаем spy, чтобы можно было переопределить отдельные методы
        return Mockito.spy(new TokenService(defaultLogin, defaultPassword, tokenRepository, webClient));
    }
}
