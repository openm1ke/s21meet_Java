package ru.izpz.edu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.izpz.dto.ApiClient;
import ru.izpz.edu.service.CampusApiProxy;
import ru.izpz.edu.service.ClusterApiProxy;
import ru.izpz.edu.service.ParticipantApiProxy;
import ru.izpz.edu.service.TokenService;

@Configuration
@ConditionalOnProperty(name = "api.client.enabled", havingValue = "true", matchIfMissing = true)
public class ApiClientConfig {

    @Bean
    public ApiClient apiClient(TokenService tokenService) {
        var apiClient = new ApiClient();
        apiClient.setApiKey(tokenService.getToken());
        apiClient.setApiKeyPrefix("Bearer");
        return apiClient;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CampusApiProxy campusApiProxy(ApiClient apiClient) {
        return new CampusApiProxy(apiClient);
    }

    @Bean
    public ClusterApiProxy clusterApiProxy(ApiClient apiClient) {
        return new ClusterApiProxy(apiClient);
    }

    @Bean
    public ParticipantApiProxy participantApiProxy(ApiClient apiClient) {
        return new ParticipantApiProxy(apiClient);
    }
}
