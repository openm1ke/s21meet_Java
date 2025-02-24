package ru.school21.edu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.school21.edu.ApiClient;
import ru.school21.edu.api.CampusApi;

@Configuration
public class CampusApiConfig {

    @Bean
    public ApiClient campusApiClient() {
        return new ApiClient();
    }

    @Bean
    public CampusApi campusApi(ApiClient campusApiClient) {
        return new CampusApi(campusApiClient);
    }
}
