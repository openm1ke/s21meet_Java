package ru.izpz.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.izpz.auth.client.TokenClient;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.auth.model.TokenEntity;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${edu.login}")
    private String defaultLogin;
    private final TokenClient tokenClient;
    private final TokenPersistenceService tokenPersistenceService;

    public String getAccessToken(String login, String password) {
        TokenResponse tr = tokenClient.requestNewToken(login, password);
        tokenPersistenceService.upsertToken(login, password, tr);
        return tr.getAccessToken();
    }

    public List<TokenEntity> findAll() {
        return tokenPersistenceService.findAll();
    }

    public Optional<TokenEntity> findById(String login) {
        return tokenPersistenceService.findById(login);
    }

    public String getDefaultAccessToken() {
        if (defaultLogin == null || defaultLogin.isBlank()) return null;
        return tokenPersistenceService.findById(defaultLogin)
            .map(TokenEntity::getAccessToken)
            .orElse(null);
    }
}

