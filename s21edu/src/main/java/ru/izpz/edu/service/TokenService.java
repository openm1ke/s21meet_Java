package ru.izpz.edu.service;

import lombok.extern.slf4j.Slf4j;
import ru.izpz.auth.exception.TokenResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@ConditionalOnProperty(name = "token.service.enabled", havingValue = "true")
public class TokenService {

    @Value("${edu.tokenEndpoint}")
    private String tokenEndpoint;

    private final RestTemplate restTemplate;

    public TokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getToken() {
        ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenEndpoint, String.class);
        String token = tokenResponse.getBody();
        if (token == null || token.isEmpty()) {
            throw new TokenResponseException("Не удалось получить access token");
        }
        return token;
    }
}
