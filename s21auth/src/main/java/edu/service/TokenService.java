package edu.service;

import edu.repository.TokenRepository;
import edu.dto.TokenResponse;
import edu.model.TokenEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.introspector.Property;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class TokenService {

    private final String defaultLogin;
    private final String defaultPassword;
    private final TokenRepository tokenRepository;
    private final WebClient webClient;

    public TokenService(
            @Value("${edu.login}") String defaultLogin,
            @Value("${edu.password}") String defaultPassword,
            TokenRepository tokenRepository,
            WebClient webClient) {
        this.defaultLogin = defaultLogin;
        this.defaultPassword = defaultPassword;
        this.tokenRepository = tokenRepository;
        this.webClient = webClient;
    }

    /**
     * Возвращает актуальный access token для заданного логина.
     * Если токен отсутствует или устарел, запрашивает новый.
     */
    public String getAccessToken(String login, String password) {
        try {
            TokenEntity tokenEntity = tokenRepository.findById(login).orElse(null);
            if (tokenEntity != null && tokenEntity.getExpiresAt() != null &&
                    tokenEntity.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
                return tokenEntity.getAccessToken();
            }
            TokenResponse tokenResponse = requestNewToken(login, password);

            TokenEntity newEntity = new TokenEntity();
            newEntity.setLogin(login);
            newEntity.setPassword(password);
            newEntity.setAccessToken(tokenResponse.getAccess_token());
            newEntity.setRefreshToken(tokenResponse.getRefresh_token());
            newEntity.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpires_in()));

            tokenRepository.save(newEntity);
            return newEntity.getAccessToken();
        } catch (Exception e) {
            log.error("Ошибка получения access token для пользователя {}: {}", login, e.getMessage());
            return null;
        }
    }

    /**
     * Отправляет POST-запрос для получения нового токена, используя WebClient.
     */
    private TokenResponse requestNewToken(String login, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", "s21-open-api");
        form.add("username", login);
        form.add("password", password);
        form.add("grant_type", "password");
        try {
            // URL для запроса токена (будет дописываться к baseUrl из WebClientConfig)
            String tokenUri = "/auth/realms/EduPowerKeycloak/protocol/openid-connect/token";
            TokenResponse tokenResponse = webClient.post()
                    .uri(tokenUri)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block(); // Получаем результат синхронно

            if (tokenResponse == null) {
                throw new RuntimeException("Получен пустой ответ от сервера токенов");
            }
            return tokenResponse;
        } catch (Exception e) {
            log.error("Ошибка запроса нового токена для пользователя {}: {}", login, e.getMessage());
            throw new RuntimeException("Не удалось получить токен", e);
        }
    }

    public String getDefaultAccessToken() {
        try {
            return getAccessToken(defaultLogin, defaultPassword);
        } catch (Exception e) {
            log.error("Ошибка получения access token для пользователя {}: {}", defaultLogin, e.getMessage());
            return null;
        }
    }

    /**
     * Периодически обновляет токены для всех сохранённых пользователей.
     * Выполняется каждые 5 минут.
     */
    @Scheduled(fixedDelay = 300000)
    public void refreshTokens() {
        try {
            tokenRepository.findAll().forEach(tokenEntity -> {
                if (tokenEntity.getExpiresAt() == null ||
                        tokenEntity.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(1))) {
                    try {
                        TokenResponse tokenResponse = requestNewToken(tokenEntity.getLogin(), tokenEntity.getPassword());
                        tokenEntity.setAccessToken(tokenResponse.getAccess_token());
                        tokenEntity.setRefreshToken(tokenResponse.getRefresh_token());
                        tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpires_in()));
                        tokenRepository.save(tokenEntity);
                        log.info("Токен для {} успешно обновлён.", tokenEntity.getLogin());
                    } catch (Exception e) {
                        log.error("Ошибка обновления токена для {}: {}", tokenEntity.getLogin(), e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Ошибка обновления токенов: {}", e.getMessage());
        }
    }

    public Optional<TokenEntity> findByLogin(String login) {
        return tokenRepository.findById(login);
    }
}

