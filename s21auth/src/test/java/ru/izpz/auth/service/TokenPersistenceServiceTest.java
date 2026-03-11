package ru.izpz.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.auth.model.TokenEntity;
import ru.izpz.auth.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenPersistenceServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenPersistenceService tokenPersistenceService;

    private static final String TEST_LOGIN = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";

    @Test
    void upsertToken_shouldCreateNewToken_whenNotExists() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        when(tokenRepository.findForUpdate(TEST_LOGIN)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(TokenEntity.class))).thenAnswer(invocation -> {
            TokenEntity token = invocation.getArgument(0);
            token.setLogin(token.getLogin());
            return token;
        });

        tokenPersistenceService.upsertToken(TEST_LOGIN, TEST_PASSWORD, tokenResponse);

        verify(tokenRepository).save(argThat(token -> 
            token != null &&
            token.getLogin().equals(TEST_LOGIN) &&
            token.getPassword().equals(TEST_PASSWORD) &&
            token.getAccessToken().equals(ACCESS_TOKEN) &&
            token.getExpiresAt() != null &&
            token.getExpiresAt().isAfter(LocalDateTime.now().minusSeconds(1))
        ));
    }

    @Test
    void upsertToken_shouldUpdateExistingToken_whenExists() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        TokenEntity existingToken = new TokenEntity();
        existingToken.setLogin(TEST_LOGIN);
        existingToken.setPassword("oldPassword");
        existingToken.setAccessToken("oldToken");

        when(tokenRepository.findForUpdate(TEST_LOGIN)).thenReturn(Optional.of(existingToken));
        when(tokenRepository.save(any(TokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tokenPersistenceService.upsertToken(TEST_LOGIN, TEST_PASSWORD, tokenResponse);

        verify(tokenRepository).save(argThat(token -> 
            token != null &&
            token.getLogin().equals(TEST_LOGIN) &&
            token.getPassword().equals(TEST_PASSWORD) &&
            token.getAccessToken().equals(ACCESS_TOKEN)
        ));
    }

    @Test
    void upsertToken_shouldHandleDataIntegrityViolationException() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        when(tokenRepository.findForUpdate(TEST_LOGIN)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(TokenEntity.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertDoesNotThrow(() -> tokenPersistenceService.upsertToken(TEST_LOGIN, TEST_PASSWORD, tokenResponse));
    }

    @Test
    void findById_shouldReturnToken_whenExists() {
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin(TEST_LOGIN);
        tokenEntity.setAccessToken(ACCESS_TOKEN);

        when(tokenRepository.findById(TEST_LOGIN)).thenReturn(Optional.of(tokenEntity));

        Optional<TokenEntity> result = tokenPersistenceService.findById(TEST_LOGIN);

        assertTrue(result.isPresent());
        assertEquals(ACCESS_TOKEN, result.get().getAccessToken());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        when(tokenRepository.findById(TEST_LOGIN)).thenReturn(Optional.empty());

        Optional<TokenEntity> result = tokenPersistenceService.findById(TEST_LOGIN);

        assertFalse(result.isPresent());
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
        when(tokenRepository.findAll()).thenReturn(expectedTokens);

        List<TokenEntity> result = tokenPersistenceService.findAll();

        assertEquals(expectedTokens, result);
    }
}
