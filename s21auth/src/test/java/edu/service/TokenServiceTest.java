package edu.service;

import edu.dto.TokenResponse;
import edu.exception.TokenResponseException;
import edu.model.TokenEntity;
import edu.repository.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private RestTemplate restTemplate;

    private static final String TEST_LOGIN = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String ACCESS_TOKEN = "newAccessToken";
    private static final String REFRESH_TOKEN = "newRefreshToken";

    @Test
    void getAccessToken_shouldReturnExistingToken() {
        TokenEntity existingToken = new TokenEntity();
        existingToken.setLogin(TEST_LOGIN);
        existingToken.setAccessToken(ACCESS_TOKEN);
        existingToken.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // Токен еще актуален

        when(tokenRepository.findById(TEST_LOGIN)).thenReturn(Optional.of(existingToken));

        String token = tokenService.getAccessToken(TEST_LOGIN, TEST_PASSWORD);

        assertEquals(ACCESS_TOKEN, token, "Метод должен вернуть существующий токен");
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class));
    }

    @Test
    void getAccessToken_shouldRequestNewTokenIfExpired() {
        TokenEntity expiredToken = new TokenEntity();
        expiredToken.setLogin(TEST_LOGIN);
        expiredToken.setAccessToken("expiredToken");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Истекший токен

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        ResponseEntity<TokenResponse> mockResponse = ResponseEntity.ok(tokenResponse);

        when(tokenRepository.findById(TEST_LOGIN)).thenReturn(Optional.of(expiredToken));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(mockResponse);

        String token = tokenService.getAccessToken(TEST_LOGIN, TEST_PASSWORD);

        assertEquals(ACCESS_TOKEN, token, "Метод должен вернуть новый токен");
        verify(tokenRepository).save(any(TokenEntity.class)); // Должен сохранить новый токен
    }

    @Test
    void getAccessToken_shouldThrowExceptionIfTokenRequestFails() {
        TokenEntity expiredToken = new TokenEntity();
        expiredToken.setLogin(TEST_LOGIN);
        expiredToken.setAccessToken("expiredToken");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Истекший токен

        when(tokenRepository.findById(TEST_LOGIN)).thenReturn(Optional.of(expiredToken));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenThrow(new RuntimeException("Ошибка сети"));

        assertThrows(TokenResponseException.class, () ->
                        tokenService.getAccessToken(TEST_LOGIN, TEST_PASSWORD),
                "Метод должен выбросить TokenResponseException"
        );

        verify(tokenRepository, never()).save(any(TokenEntity.class)); // Токен не должен быть сохранен
    }
}