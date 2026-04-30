package ru.izpz.auth.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import ru.izpz.auth.client.TokenClient;
import ru.izpz.auth.service.TokenPersistenceService;
import ru.izpz.auth.service.TokenService;

/** Тестовая конфигурация для slice-тестов контроллера токенов. */
@ActiveProfiles("test")
@TestConfiguration
public class TokenControllerConfig {

  /** Создает spy для {@link TokenService} с замоканными зависимостями. */
  @Bean
  public TokenService tokenService() {

    TokenClient tokenClient = Mockito.mock(TokenClient.class);
    TokenPersistenceService tokenPersistenceService = Mockito.mock(TokenPersistenceService.class);

    return Mockito.spy(new TokenService(tokenClient, tokenPersistenceService));
  }
}
