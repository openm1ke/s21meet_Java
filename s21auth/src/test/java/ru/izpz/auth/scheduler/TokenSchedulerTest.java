package ru.izpz.auth.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.auth.model.TokenEntity;
import ru.izpz.auth.service.TokenService;

@ExtendWith(MockitoExtension.class)
class TokenSchedulerTest {

  @Mock private TokenService tokenService;

  @InjectMocks private TokenScheduler tokenScheduler;

  private static final String DEFAULT_LOGIN = "defaultUser";
  private static final String DEFAULT_PASSWORD = "defaultPass";
  private static final String USER_1 = "user1";
  private static final String PASS_1 = "pass1";
  private static final String USER_2 = "user2";
  private static final String PASS_2 = "pass2";

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
    expiringToken.setLogin(USER_1);
    expiringToken.setPassword(PASS_1);
    expiringToken.setExpiresAt(now.plusMinutes(5)); // Expires in 5 minutes (< 10)

    TokenEntity validToken = new TokenEntity();
    validToken.setLogin(USER_2);
    validToken.setPassword(PASS_2);
    validToken.setExpiresAt(now.plusMinutes(20)); // Expires in 20 minutes (> 10)

    TokenEntity nullExpiryToken = new TokenEntity();
    nullExpiryToken.setLogin("user3");
    nullExpiryToken.setPassword("pass3");
    nullExpiryToken.setExpiresAt(null);

    List<TokenEntity> tokens = List.of(expiringToken, validToken, nullExpiryToken);
    when(tokenService.findAll()).thenReturn(tokens);

    tokenScheduler.refreshTokens();

    verify(tokenService).getAccessToken(USER_1, PASS_1);
    verify(tokenService).getAccessToken("user3", "pass3");
    verify(tokenService, never()).getAccessToken(USER_2, PASS_2);
  }

  @Test
  void refreshTokens_shouldHandleExceptionsGracefully() {
    LocalDateTime now = LocalDateTime.now();

    TokenEntity token1 = new TokenEntity();
    token1.setLogin(USER_1);
    token1.setPassword(PASS_1);
    token1.setExpiresAt(now.plusMinutes(5));

    TokenEntity token2 = new TokenEntity();
    token2.setLogin(USER_2);
    token2.setPassword(PASS_2);
    token2.setExpiresAt(now.plusMinutes(5));

    List<TokenEntity> tokens = List.of(token1, token2);
    when(tokenService.findAll()).thenReturn(tokens);

    doThrow(new RuntimeException("Token refresh failed"))
        .when(tokenService)
        .getAccessToken(USER_1, PASS_1);

    // Should not throw exception
    assertDoesNotThrow(() -> tokenScheduler.refreshTokens());

    verify(tokenService).getAccessToken(USER_1, PASS_1);
    verify(tokenService).getAccessToken(USER_2, PASS_2);
  }

  @Test
  void refreshTokens_shouldNotRefreshTokensThatAreStillValid() {
    LocalDateTime now = LocalDateTime.now();

    TokenEntity validToken = new TokenEntity();
    validToken.setLogin(USER_1);
    validToken.setPassword(PASS_1);
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
    boundaryToken.setLogin(USER_1);
    boundaryToken.setPassword(PASS_1);
    boundaryToken.setExpiresAt(now.plusMinutes(10)); // Exactly 10 minutes

    List<TokenEntity> tokens = List.of(boundaryToken);
    when(tokenService.findAll()).thenReturn(tokens);

    tokenScheduler.refreshTokens();

    verify(tokenService).getAccessToken(USER_1, PASS_1);
  }
}
