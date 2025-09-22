package ru.izpz.edu.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.client.ResilientApiClient;
import ru.izpz.edu.service.TokenService;

@Slf4j
@Configuration
public class ApiClientConfig {

    @Bean
    @ConditionalOnProperty(name = "api.client.enabled", havingValue = "true")
    public ApiClient apiClient(TokenService tokenService) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    var req = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + tokenService.getToken())
                            .build();
                    return chain.proceed(req);
                })
                .build();
        return new ResilientApiClient(client);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "campus.api.enabled", havingValue = "true")
    public CampusApi campusApi(ApiClient apiClient) {
        return new CampusApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "cluster.api.enabled", havingValue = "true")
    public ClusterApi clusterApi(ApiClient apiClient) {
        return new ClusterApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "participant.api.enabled", havingValue = "true")
    public ParticipantApi participantApi(ApiClient apiClient) {
        return new ParticipantApi(apiClient);
    }
}
