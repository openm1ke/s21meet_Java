package edu.service;

import edu.dto.TokenResponse;
import edu.exception.TokenResponseException;
import edu.model.TokenEntity;
import edu.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private TokenService tokenService;

    // Значения для defaultLogin/defaultPassword
    private final String defaultLogin = "defaultUser";
    private final String defaultPassword = "defaultPass";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(defaultLogin, defaultPassword, tokenRepository, webClient);
    }

    @Test
    void testGetAccessToken_ReturnsExistingToken() {
        // Подготовка: создаем сущность с токеном, который не истек
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin("user1");
        tokenEntity.setPassword("pass1");
        tokenEntity.setAccessToken("existingAccessToken");
        tokenEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(tokenRepository.findById("user1")).thenReturn(Optional.of(tokenEntity));

        // Выполнение тестируемого метода
        String token = tokenService.getAccessToken("user1", "pass1");

        // Проверка: должен вернуть существующий токен, а сохранение в репозитории не должно вызываться
        assertEquals("existingAccessToken", token);
        verify(tokenRepository, never()).save(any(TokenEntity.class));
    }

    @Test
    void testGetAccessToken_FetchesNewToken() {
        // Подготовка: токен не найден в репозитории
        when(tokenRepository.findById("user2")).thenReturn(Optional.empty());

        // Подготовка мока для WebClient
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("newAccessToken");
        tokenResponse.setRefreshToken("newRefreshToken");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Выполнение тестируемого метода
        String token = tokenService.getAccessToken("user2", "pass2");

        // Проверка: возвращается новый токен и вызывается метод сохранения
        assertEquals("newAccessToken", token);
        verify(tokenRepository, times(1)).save(any(TokenEntity.class));
    }

    @Test
    void testGetAccessToken_ThrowsExceptionOnError() {
        // Подготовка: репозиторий возвращает пустой результат
        when(tokenRepository.findById("user3")).thenReturn(Optional.empty());

        // Настраиваем WebClient так, чтобы он возвращал пустой Mono (или выбрасывал исключение)
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.empty());

        // Выполнение и проверка выброшенного исключения
        TokenResponseException thrown = assertThrows(TokenResponseException.class, () -> {
            tokenService.getAccessToken("user3", "pass3");
        });
        // Проверяем сообщение внешнего исключения
        assertEquals("Не удалось получить токен", thrown.getMessage());
        // Проверяем сообщение внутреннего исключения (cause)
        assertNotNull(thrown.getCause());
        assertEquals("Получен пустой ответ от сервера токенов", thrown.getCause().getMessage());
    }

    @Test
    void testGetAccessToken_requestsNewToken_whenTokenExpiresSoon() {
        // Создаем токен, который скоро истечет (expiresAt меньше, чем LocalDateTime.now().plusMinutes(1))
        TokenEntity soonExpiringToken = new TokenEntity();
        soonExpiringToken.setLogin("userSoon");
        soonExpiringToken.setPassword("passSoon");
        soonExpiringToken.setAccessToken("soonExpiringToken");
        soonExpiringToken.setExpiresAt(LocalDateTime.now().plusSeconds(30)); // менее 1 минуты до истечения

        when(tokenRepository.findById("userSoon")).thenReturn(Optional.of(soonExpiringToken));

        // Настраиваем WebClient для возврата нового токена
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("newTokenDueToSoonExpire");
        tokenResponse.setRefreshToken("newRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        String token = tokenService.getAccessToken("userSoon", "passSoon");

        // Проверка: т.к. существующий токен скоро истекает, должен быть запрошен новый
        assertEquals("newTokenDueToSoonExpire", token);
        verify(tokenRepository).save(any(TokenEntity.class));
    }

    @Test
    void testGetDefaultAccessToken_ThrowsException() {
        // Подготовка: для дефолтного пользователя в репозитории ничего нет
        when(tokenRepository.findById(defaultLogin)).thenReturn(Optional.empty());

        // Настройка WebClient, чтобы возвращался пустой Mono, что вызовет исключение
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.empty());

        // Действие и проверка: метод getDefaultAccessToken должен выбросить исключение
        TokenResponseException ex = assertThrows(TokenResponseException.class, () -> {
            tokenService.getDefaultAccessToken();
        });
        assertEquals("Не удалось получить токен", ex.getMessage());
    }

    @Test
    void refreshTokens_updatesToken_whenExpiresAtIsNull() {
        // Сценарий: у токена отсутствует время истечения
        TokenEntity tokenWithNullExpires = new TokenEntity();
        tokenWithNullExpires.setLogin("userNull");
        tokenWithNullExpires.setPassword("passNull");
        tokenWithNullExpires.setAccessToken("oldToken");
        tokenWithNullExpires.setExpiresAt(null);

        when(tokenRepository.findAll()).thenReturn(Collections.singletonList(tokenWithNullExpires));

        // Настраиваем WebClient для получения нового токена
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("updatedTokenFromNull");
        tokenResponse.setRefreshToken("updatedRefresh");
        tokenResponse.setExpiresIn(3600);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(tokenResponse));

        // Действие
        tokenService.refreshTokens();

        // Проверка: убедимся, что новый токен сохранен
        ArgumentCaptor<TokenEntity> captor = ArgumentCaptor.forClass(TokenEntity.class);
        verify(tokenRepository).save(captor.capture());
        TokenEntity savedEntity = captor.getValue();
        assertEquals("updatedTokenFromNull", savedEntity.getAccessToken());
    }

    @Test
    void refreshTokens_handlesException_whenTokenRepositoryFindAllThrows() {
        // Симулируем ошибку в методе findAll, которая должна быть поймана и не выброшена наружу
        when(tokenRepository.findAll()).thenThrow(new RuntimeException("Simulated repository error"));

        // Действие: метод refreshTokens не должен выбрасывать исключение наружу
        assertDoesNotThrow(() -> tokenService.refreshTokens());
    }

    @Test
    void testFindByLogin_ReturnsTokenEntity() {
        // Создаем сущность для пользователя
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin("testUser");
        tokenEntity.setAccessToken("testToken");

        when(tokenRepository.findById("testUser")).thenReturn(Optional.of(tokenEntity));

        // Действие
        Optional<TokenEntity> result = tokenService.findByLogin("testUser");

        // Проверка: убедимся, что Optional содержит нужный объект
        assertTrue(result.isPresent());
        assertEquals("testToken", result.get().getAccessToken());
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
}