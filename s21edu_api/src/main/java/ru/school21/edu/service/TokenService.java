package ru.school21.edu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "token.service.enabled", havingValue = "true", matchIfMissing = true)
public class TokenService {

    @Value("${edu.tokenEndpoint}")
    private String tokenEndpoint;

    public String getToken() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenEndpoint, String.class);
        String token = tokenResponse.getBody();
        if (token.isEmpty()) {
            throw new RuntimeException("Не удалось получить access token");
        }
        return token;
    }
}
