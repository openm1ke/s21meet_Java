package ru.izpz.auth.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.auth.model.TokenEntity;
import ru.izpz.auth.service.TokenService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenSchedulerTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TokenScheduler tokenScheduler;

    private static final String DEFAULT_LOGIN = "defaultUser";
    private static final String DEFAULT_PASSWORD = "defaultPass";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenScheduler, "defaultLogin", DEFAULT_LOGIN);
        ReflectionTestUtils.setField(tokenScheduler, "defaultPassword", DEFAULT_PASSWORD);
    }

    @Test
    void init_shouldCallTokenServiceForDefaultUser() {
        tokenScheduler.init();

        verify(tokenService).getAccessToken(DEFAULT_LOGIN, DEFAULT_PASSWORD);
    }

    @Test
    void refreshTokens_shouldRefreshTokensThatAreExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        
        TokenEntity expiringToken = new TokenEntity();
        expiringToken.setLogin("user1");
        expiringToken.setPassword("pass1");
        expiringToken.setExpiresAt(now.plusMinutes(5)); // Expires in 5 minutes (< 10)

        TokenEntity validToken = new TokenEntity();
        validToken.setLogin("user2");
        validToken.setPassword("pass2");
        validToken.setExpiresAt(now.plusMinutes(20)); // Expires in 20 minutes (> 10)

        TokenEntity nullExpiryToken = new TokenEntity();
        nullExpiryToken.setLogin("user3");
        nullExpiryToken.setPassword("pass3");
        nullExpiryToken.setExpiresAt(null);

        List<TokenEntity> tokens = List.of(expiringToken, validToken, nullExpiryToken);
        when(tokenService.findAll()).thenReturn(tokens);

        tokenScheduler.refreshTokens();

        verify(tokenService).getAccessToken("user1", "pass1");
        verify(tokenService).getAccessToken("user3", "pass3");
        verify(tokenService, never()).getAccessToken("user2", "pass2");
    }

    @Test
    void refreshTokens_shouldHandleExceptionsGracefully() {
        LocalDateTime now = LocalDateTime.now();
        
        TokenEntity token1 = new TokenEntity();
        token1.setLogin("user1");
        token1.setPassword("pass1");
        token1.setExpiresAt(now.plusMinutes(5));

        TokenEntity token2 = new TokenEntity();
        token2.setLogin("user2");
        token2.setPassword("pass2");
        token2.setExpiresAt(now.plusMinutes(5));

        List<TokenEntity> tokens = List.of(token1, token2);
        when(tokenService.findAll()).thenReturn(tokens);
        
        doThrow(new RuntimeException("Token refresh failed"))
                .when(tokenService).getAccessToken("user1", "pass1");

        // Should not throw exception
        assertDoesNotThrow(() -> tokenScheduler.refreshTokens());

        verify(tokenService).getAccessToken("user1", "pass1");
        verify(tokenService).getAccessToken("user2", "pass2");
    }

    @Test
    void refreshTokens_shouldNotRefreshTokensThatAreStillValid() {
        LocalDateTime now = LocalDateTime.now();
        
        TokenEntity validToken = new TokenEntity();
        validToken.setLogin("user1");
        validToken.setPassword("pass1");
        validToken.setExpiresAt(now.plusMinutes(15)); // Still valid for 15 minutes

        List<TokenEntity> tokens = List.of(validToken);
        when(tokenService.findAll()).thenReturn(tokens);

        tokenScheduler.refreshTokens();

        verify(tokenService, never()).getAccessToken(anyString(), anyString());
    }

    @Test
    void refreshTokens_shouldHandleEmptyTokenList() {
        when(tokenService.findAll()).thenReturn(List.of());

        tokenScheduler.refreshTokens();

        verify(tokenService, never()).getAccessToken(anyString(), anyString());
    }

    @Test
    void refreshTokens_shouldRefreshTokensExactlyAt10MinuteBoundary() {
        LocalDateTime now = LocalDateTime.now();
        
        TokenEntity boundaryToken = new TokenEntity();
        boundaryToken.setLogin("user1");
        boundaryToken.setPassword("pass1");
        boundaryToken.setExpiresAt(now.plusMinutes(10)); // Exactly 10 minutes

        List<TokenEntity> tokens = List.of(boundaryToken);
        when(tokenService.findAll()).thenReturn(tokens);

        tokenScheduler.refreshTokens();

        verify(tokenService).getAccessToken("user1", "pass1");
    }
}
