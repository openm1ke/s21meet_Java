package ru.izpz.auth.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.exception.TokenResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenClientTest {

    @Mock
    private RestTemplate restTemplate;
    
    private TokenClient tokenClient;

    private static final String TEST_LOGIN = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9";
    private static final String REFRESH_TOKEN = "refreshToken";

    @BeforeEach
    void setUp() {
        tokenClient = new TokenClient("/auth/realms/EduPowerKeycloak/protocol/openid-connect/token", restTemplate);
    }

    @Test
    void requestNewToken_shouldReturnTokenResponse_whenResponseIsValid() {
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken(ACCESS_TOKEN);
        expectedResponse.setRefreshToken(REFRESH_TOKEN);
        expectedResponse.setExpiresIn(3600);

        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(expectedResponse);
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        TokenResponse result = tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD);

        assertNotNull(result);
        assertEquals(ACCESS_TOKEN, result.getAccessToken());
        assertEquals(REFRESH_TOKEN, result.getRefreshToken());
        assertEquals(3600, result.getExpiresIn());
    }

    @Test
    void requestNewToken_shouldThrowException_whenResponseBodyIsNull() {
        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(null);
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        assertThrows(TokenResponseException.class, () -> 
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );
    }

    @Test
    void requestNewToken_shouldThrowException_whenResponseHasNoBody() {
        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.noContent().build();
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        assertThrows(TokenResponseException.class, () ->
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );
    }

    @Test
    void requestNewToken_shouldThrowException_whenHasBodyTrueButBodyNull() {
        @SuppressWarnings("unchecked")
        ResponseEntity<TokenResponse> responseEntity = mock(ResponseEntity.class);
        when(responseEntity.hasBody()).thenReturn(true);
        when(responseEntity.getBody()).thenReturn(null);
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        assertThrows(TokenResponseException.class, () ->
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );
    }

    @Test
    void requestNewToken_shouldThrowException_whenAccessTokenIsNull() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(null);
        tokenResponse.setRefreshToken(REFRESH_TOKEN);
        tokenResponse.setExpiresIn(3600);

        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(tokenResponse);
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        assertThrows(TokenResponseException.class, () -> 
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );
    }

    @Test
    void requestNewToken_shouldThrowException_whenRestClientExceptionOccurs() {
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenThrow(new RestClientException("Connection error"));

        TokenResponseException exception = assertThrows(TokenResponseException.class, () -> 
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );

        assertEquals("Не удалось получить токен", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void requestNewToken_shouldThrowException_whenGenericExceptionOccurs() {
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        TokenResponseException exception = assertThrows(TokenResponseException.class, () -> 
                tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD)
        );

        assertEquals("Не удалось получить токен", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void requestNewToken_shouldUseCorrectEndpoint() {
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken(ACCESS_TOKEN);
        expectedResponse.setRefreshToken(REFRESH_TOKEN);
        expectedResponse.setExpiresIn(3600);

        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(expectedResponse);
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        tokenClient.requestNewToken(TEST_LOGIN, TEST_PASSWORD);

        verify(restTemplate).postForEntity(contains("token"), any(), eq(TokenResponse.class));
    }
}
