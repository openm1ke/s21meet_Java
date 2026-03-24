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
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.api.ProjectApi;
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
        return new ApiClient(client);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "api.campus.enabled", havingValue = "true")
    public CampusApi campusApi(ApiClient apiClient) {
        return new CampusApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "api.cluster.enabled", havingValue = "true")
    public ClusterApi clusterApi(ApiClient apiClient) {
        return new ClusterApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "api.participant.enabled", havingValue = "true")
    public ParticipantApi participantApi(ApiClient apiClient) {
        return new ParticipantApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "api.coalition.enabled", havingValue = "true")
    public CoalitionApi coalitionApi(ApiClient apiClient) {
        return new CoalitionApi(apiClient);
    }

    @Bean
    public EventApi eventApi(ApiClient apiClient) {
        return new EventApi(apiClient);
    }

    @Bean
    @ConditionalOnProperty(name = "api.project.enabled", havingValue = "true")
    public ProjectApi projectApi(ApiClient apiClient) {
        return new ProjectApi(apiClient);
    }
}
