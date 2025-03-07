package edu.service;

import edu.dto.TokenResponse;
import edu.model.TokenEntity;
import edu.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private WebClient webClient;

    // Моки для цепочки вызовов WebClient
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private TokenService tokenService;

    // Значения для defaultLogin/defaultPassword
    private final String defaultLogin = "defaultUser";
    private final String defaultPassword = "defaultPass";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(defaultLogin, defaultPassword, tokenRepository, webClient);
    }

    @Test
    void getAccessToken() {
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin("user1");
        tokenEntity.setAccessToken("cachedToken");
        tokenEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // не просрочен

        when(tokenRepository.findById("user1")).thenReturn(Optional.of(tokenEntity));

        // Действие
        String accessToken = tokenService.getAccessToken("user1", "pass");

        // Проверка: должен вернуться кешированный токен, без вызова WebClient
        assertEquals("cachedToken", accessToken);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void getAccessToken_requestsNewToken_whenNoCachedToken() {
        // Сценарий: токен отсутствует в репозитории
        when(tokenRepository.findById("user2")).thenReturn(Optional.empty());

        // Подготавливаем ответ от WebClient
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("newToken");
        tokenResponse.setRefreshToken("newRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/auth/realms/EduPowerKeycloak/protocol/openid-connect/token"))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        String accessToken = tokenService.getAccessToken("user2", "pass2");

        // Проверка: должен вернуться новый токен, а также вызвать сохранение в репозитории
        assertEquals("newToken", accessToken);
        verify(tokenRepository).save(any(TokenEntity.class));
    }

    @Test
    void getAccessToken_requestsNewToken_whenCachedTokenExpired() {
        // Сценарий: репозиторий возвращает просроченный токен
        TokenEntity expiredToken = new TokenEntity();
        expiredToken.setLogin("user3");
        expiredToken.setAccessToken("oldToken");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // просрочен

        when(tokenRepository.findById("user3")).thenReturn(Optional.of(expiredToken));

        // Подготавливаем новый ответ от WebClient
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("updatedToken");
        tokenResponse.setRefreshToken("updatedRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/auth/realms/EduPowerKeycloak/protocol/openid-connect/token"))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        String accessToken = tokenService.getAccessToken("user3", "pass3");

        // Проверка: должен вернуться обновленный токен
        assertEquals("updatedToken", accessToken);
        verify(tokenRepository).save(argThat(token -> "updatedToken".equals(token.getAccessToken())));
    }

    @Test
    void getDefaultAccessToken_callsGetAccessTokenWithDefaultCredentials() {
        // Сценарий: для default пользователя репозиторий возвращает пустой результат
        when(tokenRepository.findById(defaultLogin)).thenReturn(Optional.empty());

        // Подготавливаем ответ от WebClient для дефолтного пользователя
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("defaultToken");
        tokenResponse.setRefreshToken("defaultRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/auth/realms/EduPowerKeycloak/protocol/openid-connect/token"))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        String defaultAccessToken = tokenService.getDefaultAccessToken();

        // Проверка: должен вернуться дефолтный токен
        assertEquals("defaultToken", defaultAccessToken);
    }

    @Test
    void refreshTokens_updatesExpiredToken() {
        // Сценарий: репозиторий содержит просроченный токен
        TokenEntity expiredToken = new TokenEntity();
        expiredToken.setLogin("user4");
        expiredToken.setPassword("pass4");
        expiredToken.setAccessToken("expiredToken");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(2));

        when(tokenRepository.findAll()).thenReturn(Collections.singletonList(expiredToken));

        // Подготавливаем ответ от WebClient для обновления токена
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("refreshedToken");
        tokenResponse.setRefreshToken("refreshedRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/auth/realms/EduPowerKeycloak/protocol/openid-connect/token"))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        tokenService.refreshTokens();

        // Проверка: репозиторий должен сохранить обновлённый токен
        ArgumentCaptor<TokenEntity> captor = ArgumentCaptor.forClass(TokenEntity.class);
        verify(tokenRepository).save(captor.capture());
        TokenEntity savedEntity = captor.getValue();
        assertEquals("refreshedToken", savedEntity.getAccessToken());
    }

    @Test
    void getAccessToken_returnsNull_whenWebClientThrowsException() {
        when(tokenRepository.findById("userX")).thenReturn(Optional.empty());

        // Настраиваем цепочку вызовов WebClient
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Simulated error")));

        String result = tokenService.getAccessToken("userX", "passX");

        assertNull(result);
    }

    @Test
    void refreshTokens_handlesException_whenTokenRefreshFails() {
        TokenEntity expiredToken = new TokenEntity();
        expiredToken.setLogin("userY");
        expiredToken.setPassword("passY");
        expiredToken.setAccessToken("oldToken");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(2));

        when(tokenRepository.findAll()).thenReturn(Collections.singletonList(expiredToken));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Simulated refresh error")));

        assertDoesNotThrow(() -> tokenService.refreshTokens());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void getDefaultAccessToken_returnsNull_whenExceptionOccurs() {
        when(tokenRepository.findById(defaultLogin)).thenReturn(Optional.empty());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Simulated error for default user")));

        String result = tokenService.getDefaultAccessToken();

        assertNull(result);
    }
}