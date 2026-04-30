package ru.izpz.edu.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.client.RestPlatformApiFacade;
import ru.izpz.edu.service.TokenService;

class ApiClientConfigTest {

  private final ApiClientConfig config = new ApiClientConfig();

  @Test
  void restTemplate_shouldCreateNewInstance() {
    RestTemplate restTemplate = config.restTemplate();
    assertNotNull(restTemplate);
  }

  @Test
  void restPlatformBeans_shouldBeCreated() {
    TokenService tokenService = mock(TokenService.class);
    when(tokenService.getToken()).thenReturn("test-token");

    RestClient restClient = config.platformRestClient(tokenService);
    PlatformApiFacade facade = config.restPlatformApiFacade(restClient);

    assertNotNull(restClient);
    assertInstanceOf(RestPlatformApiFacade.class, facade);
  }
}
