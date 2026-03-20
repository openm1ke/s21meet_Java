package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.exception.TokenResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TokenService.class, RestTemplate.class})
@TestPropertySource(properties = "edu.tokenEndpoint=http://localhost:8081/api/tokens/default")
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Создаем сервер, который будет перехватывать вызовы RestTemplate
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGetTokenReturnsToken() {
        String expectedToken = "myAccessToken";
        // Настраиваем ожидание вызова и ответ
        mockServer.expect(requestTo("http://localhost:8081/api/tokens/default"))
                .andRespond(withSuccess(expectedToken, MediaType.TEXT_PLAIN));

        // Выполняем вызов сервиса
        String token = tokenService.getToken();
        assertEquals(expectedToken, token);

        // Проверяем, что все ожидания сервера выполнены
        mockServer.verify();
    }

    @Test
    void testGetTokenThrowsExceptionWhenEmptyResponse() {
        // Настраиваем ответ с пустым телом
        mockServer.expect(requestTo("http://localhost:8081/api/tokens/default"))
                .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

        // Ожидаем, что будет выброшено исключение
        Exception ex = assertThrows(TokenResponseException.class, () -> tokenService.getToken());
        assertEquals("Не удалось получить access token", ex.getMessage());

        mockServer.verify();
    }

    @Test
    void testGetTokenThrowsExceptionWhenNullResponseBody() {
        RestTemplate mockedRestTemplate = mock(RestTemplate.class);
        TokenService service = new TokenService(mockedRestTemplate);
        ReflectionTestUtils.setField(service, "tokenEndpoint", "http://localhost:8081/api/tokens/default");

        when(mockedRestTemplate.getForEntity("http://localhost:8081/api/tokens/default", String.class))
                .thenReturn(ResponseEntity.ok().build());

        Exception ex = assertThrows(TokenResponseException.class, service::getToken);
        assertEquals("Не удалось получить access token", ex.getMessage());
    }
}
