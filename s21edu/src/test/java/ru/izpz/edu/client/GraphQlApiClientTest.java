package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.edu.service.TokenService;

@ExtendWith(MockitoExtension.class)
class GraphQlApiClientTest {
  private static final ObjectMapper TEST_OM = new ObjectMapper();

  @Mock private RestTemplate restTemplate;

  @Mock private TokenService tokenService;

  @Mock private ObjectMapper objectMapper;

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

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
    var errorCounter =
        meterRegistry
            .find("edu_graphql_requests_total")
            .tag("operation", "op")
            .tag("outcome", "error")
            .counter();
    assertNotNull(errorCounter);
    assertEquals(1.0, errorCounter.count());
  }

  @Test
  void execute_shouldThrow_whenResponseBodyBlank() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(""));

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldThrow_whenResponseBodyNull() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok().body(null));

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldThrow_whenJsonParseFails() throws Exception {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("notjson"));
    when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("bad") {});

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldThrow_whenResponseContainsErrors() throws Exception {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"errors\":[{\"message\":\"boom\"}]}"));
    JsonNode errorNode = jsonNode("{\"errors\":[{\"message\":\"boom\"}]}");
    when(objectMapper.readTree(anyString())).thenReturn(errorNode);

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldIgnoreEmptyErrorsArray_andReturnData() throws Exception {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"errors\":[],\"data\":{\"field\":\"value\"}}"));
    JsonNode root = jsonNode("{\"errors\":[],\"data\":{\"field\":\"value\"}}");
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
    JsonNode root = jsonNode("{\"errors\":{},\"data\":{\"field\":\"value\"}}");
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
        .thenReturn(TEST_OM.createObjectNode().set("data", TEST_OM.nullNode()));

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldThrow_whenDataFieldAbsent() throws Exception {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"meta\":1}"));
    when(objectMapper.readTree(anyString())).thenReturn(TEST_OM.createObjectNode().put("meta", 1));

    assertThrows(GraphQlRemoteException.class, this::executeDefaultOperation);
  }

  @Test
  void execute_shouldReturnData_whenValidResponse() throws Exception {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"data\":{\"field\":\"value\"}}"));
    when(objectMapper.readTree(anyString()))
        .thenReturn(
            TEST_OM
                .createObjectNode()
                .set("data", TEST_OM.createObjectNode().put("field", "value")));
    when(objectMapper.convertValue(any(), eq(String.class))).thenReturn("value");

    String result = client.execute("op", Map.of(), "query", String.class);

    assertEquals("value", result);
    var successCounter =
        meterRegistry
            .find("edu_graphql_requests_total")
            .tag("operation", "op")
            .tag("outcome", "success")
            .counter();
    assertNotNull(successCounter);
    assertEquals(1.0, successCounter.count());
    assertNotNull(
        meterRegistry
            .find("edu_graphql_request_duration_seconds")
            .tag("operation", "op")
            .tag("outcome", "success")
            .timer());
  }

  private String executeDefaultOperation() {
    return client.execute("op", Map.of(), "query", String.class);
  }

  private static JsonNode jsonNode(String json) throws JsonProcessingException {
    return TEST_OM.readTree(json);
  }
}
