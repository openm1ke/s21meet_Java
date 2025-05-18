package edu.service;

import edu.dto.TokenResponse;
import edu.exception.TokenResponseException;
import edu.model.TokenEntity;
import edu.repository.TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class TokenService {

    private final String defaultLogin;
    private final String defaultPassword;
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    public TokenService(
            @Value("${edu.login}") String defaultLogin,
            @Value("${edu.password}") String defaultPassword,
            TokenRepository tokenRepository,
            RestTemplate restTemplate) {
        this.defaultLogin = defaultLogin;
        this.defaultPassword = defaultPassword;
        this.tokenRepository = tokenRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Возвращает актуальный access token для заданного логина.
     * Если токен отсутствует или устарел, запрашивает новый.
     */
    public String getAccessToken(String login, String password) {
        TokenEntity tokenEntity = tokenRepository.findById(login).orElse(null);

        if (tokenEntity != null && tokenEntity.getExpiresAt() != null &&
                tokenEntity.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return tokenEntity.getAccessToken();
        }

        try {
            TokenResponse tokenResponse = requestNewToken(login, password);

            TokenEntity newEntity = new TokenEntity();
            newEntity.setLogin(login);
            newEntity.setPassword(password);
            newEntity.setAccessToken(tokenResponse.getAccessToken());
            newEntity.setRefreshToken(tokenResponse.getRefreshToken());
            newEntity.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));

            tokenRepository.save(newEntity);
            return newEntity.getAccessToken();
        } catch (TokenResponseException e) {
            log.error("Ошибка получения access token для {}: {}", login, e.getMessage(), e);
            throw e;
        }
    }
    /**
     * Отправляет POST-запрос для получения нового токена, используя RestTemplate.
     */
    private TokenResponse requestNewToken(String login, String password) {
        String tokenUri = "/auth/realms/EduPowerKeycloak/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "s21-open-api");
        body.add("username", login);
        body.add("password", password);
        body.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUri, requestEntity, TokenResponse.class);
            if (response.hasBody()) {
                TokenResponse tokenResponse = response.getBody();
                if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                    log.info("Получен новый токен для {}: {}", login, tokenResponse.getAccessToken().substring(0, 10) + "...");
                    return tokenResponse;
                }
            }
            log.warn("Пустой ответ при получении токена для {}", login);
            throw new TokenResponseException("Не удалось получить токен — пустой ответ");
        } catch (Exception e) {
            log.error("Ошибка запроса нового токена для {}: {}", login, e.getMessage(), e);
            throw new TokenResponseException("Не удалось получить токен", e);
        }
    }

    public String getDefaultAccessToken() {
        return getAccessToken(defaultLogin, defaultPassword);
    }

    /**
     * Периодически обновляет токены для всех сохранённых пользователей.
     * Выполняется каждые 5 минут.
     */
    @Scheduled(fixedDelay = 300000)
    public void refreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findAll().forEach(tokenEntity -> {
            if (tokenEntity.getExpiresAt() == null ||
                    tokenEntity.getExpiresAt().isBefore(now.plusMinutes(1))) {
                try {
                    TokenResponse tokenResponse = requestNewToken(tokenEntity.getLogin(), tokenEntity.getPassword());
                    tokenEntity.setAccessToken(tokenResponse.getAccessToken());
                    tokenEntity.setRefreshToken(tokenResponse.getRefreshToken());
                    tokenEntity.setExpiresAt(now.plusSeconds(tokenResponse.getExpiresIn()));
                    tokenRepository.save(tokenEntity);
                    log.info("Токен для {} успешно обновлён.", tokenEntity.getLogin());
                } catch (Exception e) {
                    log.error("Ошибка обновления токена для {}: {}", tokenEntity.getLogin(), e.getMessage(), e);
                }
            }
        });
    }

    public Optional<TokenEntity> findByLogin(String login) {
        return tokenRepository.findById(login);
    }
}

