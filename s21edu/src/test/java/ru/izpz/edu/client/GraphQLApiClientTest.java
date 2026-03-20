package ru.izpz.edu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private GraphQLApiClient client;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        client = new GraphQLApiClient(restTemplate, tokenService, objectMapper, meterRegistry);
    }

    @Test
    void execute_shouldThrow_whenRestTemplateThrowsHttpError() {
        when(tokenService.getToken()).thenReturn("tok");
        HttpStatusCodeException ex = new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {};
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(ex);

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
        assertEquals(1.0, meterRegistry.find("edu_graphql_requests_total")
                .tag("domain", "platform")
                .tag("operation", "op")
                .tag("outcome", "error")
                .counter()
                .count());
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
    void execute_shouldThrow_whenResponseBodyNull() {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().body(null));

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
        ObjectMapper om = new ObjectMapper();
        JsonNode errorNode = om.readTree("{\"errors\":[{\"message\":\"boom\"}]}");
        when(objectMapper.readTree(anyString())).thenReturn(errorNode);

        assertThrows(GraphQLApiClient.GraphQlRemoteException.class,
                () -> client.execute("op", Map.of(), "query", String.class));
    }

    @Test
    void execute_shouldIgnoreEmptyErrorsArray_andReturnData() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"errors\":[],\"data\":{\"field\":\"value\"}}"));
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree("{\"errors\":[],\"data\":{\"field\":\"value\"}}");
        when(objectMapper.readTree(anyString())).thenReturn(root);
        when(objectMapper.convertValue(any(), eq(String.class))).thenReturn("value");

        String result = client.execute("op", Map.of(), "query", String.class);

        assertEquals("value", result);
    }

    @Test
    void execute_shouldIgnoreNonArrayErrors_andReturnData() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"errors\":{},\"data\":{\"field\":\"value\"}}"));
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree("{\"errors\":{},\"data\":{\"field\":\"value\"}}");
        when(objectMapper.readTree(anyString())).thenReturn(root);
        when(objectMapper.convertValue(any(), eq(String.class))).thenReturn("value");

        String result = client.execute("op", Map.of(), "query", String.class);

        assertEquals("value", result);
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
    void execute_shouldThrow_whenDataFieldAbsent() throws Exception {
        when(tokenService.getToken()).thenReturn("tok");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"meta\":1}"));
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode().put("meta", 1));

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
        assertEquals(1.0, meterRegistry.find("edu_graphql_requests_total")
                .tag("domain", "platform")
                .tag("operation", "op")
                .tag("outcome", "success")
                .counter()
                .count());
        assertNotNull(meterRegistry.find("edu_graphql_request_duration_seconds")
                .tag("domain", "platform")
                .tag("operation", "op")
                .tag("outcome", "success")
                .timer());
    }
}
