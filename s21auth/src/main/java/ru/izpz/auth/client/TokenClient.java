package ru.izpz.auth.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.exception.TokenResponseException;

@Slf4j
@Service
public class TokenClient {

  private final RestTemplate restTemplate;
  private final String tokenUri;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "RestTemplate is a Spring-managed bean injected by DI and is not mutated by TokenClient")
  public TokenClient(@Value("${token.uri}") String tokenUri, RestTemplate restTemplate) {
    this.tokenUri = tokenUri;
    this.restTemplate = restTemplate;
  }

  @RateLimiter(name = "auth")
  @Retry(name = "auth")
  @CircuitBreaker(name = "auth")
  public TokenResponse requestNewToken(String login, String password) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", "s21-open-api");
    body.add("username", login);
    body.add("password", password);
    body.add("grant_type", "password");

    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
    try {
      ResponseEntity<TokenResponse> response =
          restTemplate.postForEntity(tokenUri, requestEntity, TokenResponse.class);
      if (response.hasBody()) {
        TokenResponse tokenResponse = response.getBody();
        if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
          String accessToken = tokenResponse.getAccessToken();
          log.info(
              "Получен новый токен для {}: {}",
              login,
              accessToken.substring(0, Math.min(accessToken.length(), 10)) + "...");
          return tokenResponse;
        }
      }
      log.warn("Пустой ответ при получении токена для {}", login);
      throw new TokenResponseException("Не удалось получить токен — пустой ответ");
    } catch (HttpServerErrorException | ResourceAccessException e) {
      log.error("Временная ошибка при запросе токена для {}: {}", login, e.getMessage(), e);
      throw e;
    } catch (RestClientException e) {
      log.error("Ошибка запроса нового токена для {}: {}", login, e.getMessage(), e);
      throw new TokenResponseException("Не удалось получить токен", e);
    } catch (RuntimeException e) {
      log.error("Непредвиденная ошибка запроса токена для {}: {}", login, e.getMessage(), e);
      throw new TokenResponseException("Не удалось получить токен", e);
    }
  }
}
