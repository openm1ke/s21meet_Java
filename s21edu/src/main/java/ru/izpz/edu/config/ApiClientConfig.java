package ru.izpz.edu.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.client.PlatformApiClient;
import ru.izpz.edu.service.TokenService;

import java.time.Duration;

@Slf4j
@Configuration
public class ApiClientConfig {

    @Value("${api.client.connect-timeout:PT5S}")
    private Duration connectTimeout = Duration.ofSeconds(5);
    @Value("${api.client.read-timeout:PT20S}")
    private Duration readTimeout = Duration.ofSeconds(20);
    @Value("${api.client.call-timeout:PT30S}")
    private Duration callTimeout = Duration.ofSeconds(30);

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
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .callTimeout(callTimeout)
            .build();
        return new PlatformApiClient(client);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(Math.max(1, connectTimeout.toMillis())));
        requestFactory.setReadTimeout(Math.toIntExact(Math.max(1, readTimeout.toMillis())));
        return new RestTemplate(requestFactory);
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

}
