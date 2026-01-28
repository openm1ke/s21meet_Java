package ru.izpz.auth.service;

import ru.izpz.auth.client.TokenClient;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.auth.exception.TokenResponseException;
import ru.izpz.auth.model.TokenEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private TokenPersistenceService tokenPersistenceService;

    @Mock
    private TokenClient tokenClient;

    private static final String TEST_LOGIN = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String ACCESS_TOKEN = "newAccessToken";
    private static final String REFRESH_TOKEN = "newRefreshToken";

    @Test
    void getAccessToken_shouldReturnNewToken() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        when(tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)).thenReturn(tokenResponse);

        String token = tokenService.getAccessToken(TEST_LOGIN, TEST_PASSWORD);

        assertEquals(ACCESS_TOKEN, token, "Метод должен вернуть новый токен");
        verify(tokenPersistenceService).upsertToken(TEST_LOGIN, TEST_PASSWORD, tokenResponse);
    }

    @Test
    void getAccessToken_shouldThrowExceptionIfTokenRequestFails() {
        when(tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD))
                .thenThrow(new TokenResponseException("Ошибка сети"));

        assertThrows(TokenResponseException.class, () ->
                        tokenService.getAccessToken(TEST_LOGIN, TEST_PASSWORD),
                "Метод должен выбросить TokenResponseException"
        );

        verify(tokenPersistenceService, never()).upsertToken(anyString(), anyString(), any(TokenResponse.class));
    }

    @Test
    void findAll_shouldReturnAllTokens() {
        TokenEntity token1 = new TokenEntity();
        token1.setLogin("user1");
        token1.setAccessToken("token1");
        
        TokenEntity token2 = new TokenEntity();
        token2.setLogin("user2");
        token2.setAccessToken("token2");

        List<TokenEntity> expectedTokens = List.of(token1, token2);
        when(tokenPersistenceService.findAll()).thenReturn(expectedTokens);

        List<TokenEntity> result = tokenService.findAll();

        assertEquals(expectedTokens, result, "Должен вернуть все токены");
    }

    @Test
    void findByLogin_shouldReturnTokenIfExists() {
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin(TEST_LOGIN);
        tokenEntity.setAccessToken(ACCESS_TOKEN);

        when(tokenPersistenceService.findById(TEST_LOGIN)).thenReturn(Optional.of(tokenEntity));

        Optional<TokenEntity> result = tokenService.findById(TEST_LOGIN);

        assertTrue(result.isPresent(), "Токен должен быть найден");
        assertEquals(ACCESS_TOKEN, result.get().getAccessToken(), "Токен должен совпадать");
    }

    @Test
    void findByLogin_shouldReturnEmptyIfNotExists() {
        when(tokenPersistenceService.findById(TEST_LOGIN)).thenReturn(Optional.empty());

        Optional<TokenEntity> result = tokenService.findById(TEST_LOGIN);

        assertFalse(result.isPresent(), "Токен не должен быть найден");
    }

    @Test
    void getDefaultAccessToken_shouldReturnNullIfDefaultLoginIsNull() {
        // В тестовом окружении defaultLogin будет null
        String result = tokenService.getDefaultAccessToken();
        assertNull(result, "Должен вернуть null если defaultLogin равен null");
    }
}