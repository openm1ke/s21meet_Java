package ru.izpz.edu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.edu.service.TokenService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TokenService tokenService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GraphQLApiClient client;

    @Test
    void execute_shouldThrow_whenRestTemplateThrowsHttpError() {
        when(tokenService.getToken()).thenReturn("tok");
        HttpStatusCodeException ex = new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {};
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(ex);

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldThrow_whenResponseBodyBlank() {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(""));

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldThrow_whenJsonParseFails() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("notjson"));
        when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("bad") {});

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldThrow_whenResponseContainsErrors() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"errors\":[{\"message\":\"boom\"}]}"));
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode().set("errors", new ObjectMapper().createArrayNode().add(new ObjectMapper().createObjectNode().put("message", "boom"))));

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldThrow_whenDataMissing() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":null}"));
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode().set("data", new ObjectMapper().nullNode()));

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldReturnData_whenValidResponse() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":{\"field\":\"value\"}}"));
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode().set("data", new ObjectMapper().createObjectNode().put("field", "value")));
        when(objectMapper.convertValue(any(), eq(String.class))).thenReturn("value");

        String result = client.execute("op", Map.of(), "query", String.class);

        assertEquals("value", result);
    }
}
