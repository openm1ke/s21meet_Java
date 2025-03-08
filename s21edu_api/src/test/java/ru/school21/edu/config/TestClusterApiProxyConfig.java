package ru.school21.edu.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import ru.school21.edu.ApiClient;
import ru.school21.edu.service.ClusterApiProxy;
import ru.school21.edu.service.TokenService;

@ActiveProfiles("test")
@TestConfiguration
public class TestClusterApiProxyConfig {
    @Bean
    public TokenService tokenService() {
        // Создаем mock для TokenService, задаем поведение для getToken()
        TokenService tokenService = Mockito.mock(TokenService.class);
        Mockito.when(tokenService.getToken()).thenReturn("token");
        return tokenService;
    }

    @Bean
    public ApiClient apiClient(TokenService tokenService) {
        var apiClient = new ApiClient();
        apiClient.setApiKey(tokenService.getToken());
        apiClient.setApiKeyPrefix("Bearer");
        return apiClient;
    }

    @Bean
    public ClusterApiProxy clusterApiProxy(ApiClient apiClient) {
        return Mockito.spy(new ClusterApiProxy(apiClient));
    }
}
