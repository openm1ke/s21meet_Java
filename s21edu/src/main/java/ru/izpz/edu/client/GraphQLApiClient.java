package ru.izpz.edu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.edu.service.TokenService;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "api.graphql.enabled", havingValue = "true")
@RequiredArgsConstructor
public class GraphQLApiClient {

    private static final String GRAPHQL_URL = "https://platform.21-school.ru/services/graphql";
    private static final String SCHOOL_ID = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
    private static final String ERRORS_FIELD = "errors";
    private static final String DATA_FIELD = "data";
    private static final String GRAPHQL_REQUEST_COUNTER = "edu_graphql_requests_total";
    private static final String GRAPHQL_REQUEST_DURATION = "edu_graphql_request_duration_seconds";
    private static final String TAG_OPERATION = "operation";
    private static final String TAG_OUTCOME = "outcome";
    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    private final ObjectMapper om;
    private final MeterRegistry meterRegistry;

    public <T> T execute(String operationName,
                         Map<String, Object> variables,
                         String query,
                         Class<T> dataClass) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            String token = tokenService.getToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            headers.add("schoolId", SCHOOL_ID);

            GraphQlRequest body = new GraphQlRequest(operationName, variables, query);
            HttpEntity<GraphQlRequest> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = executePost(entity);

            String raw = resp.getBody();
            if (raw == null || raw.isBlank()) {
                throw new GraphQlRemoteException("Пустой ответ GraphQL");
            }

            return parseAndConvert(raw, dataClass);
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            meterRegistry.counter(
                    GRAPHQL_REQUEST_COUNTER,
                    TAG_OPERATION, operationName,
                    TAG_OUTCOME, outcome
            ).increment();
            sample.stop(Timer.builder(GRAPHQL_REQUEST_DURATION)
                    .description("Duration of GraphQL requests")
                    .tag(TAG_OPERATION, operationName)
                    .tag(TAG_OUTCOME, outcome)
                    .register(meterRegistry));
        }
    }

    private ResponseEntity<String> executePost(HttpEntity<GraphQlRequest> entity) {
        try {
            return restTemplate.postForEntity(GRAPHQL_URL, entity, String.class);
        } catch (HttpStatusCodeException e) {
            throw new GraphQlRemoteException("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }

    private <T> T parseAndConvert(String raw, Class<T> dataClass) {
        try {
            JsonNode root = om.readTree(raw);
            if (root.has(ERRORS_FIELD) && root.get(ERRORS_FIELD).isArray() && !root.get(ERRORS_FIELD).isEmpty()) {
                return handleGraphQlErrors(root.get(ERRORS_FIELD));
            }
            JsonNode data = root.get(DATA_FIELD);
            if (data == null || data.isNull()) {
                throw new GraphQlRemoteException("В ответе нет поля '" + DATA_FIELD + "'");
            }
            return om.convertValue(data, dataClass);
        } catch (JsonProcessingException e) {
            throw new GraphQlRemoteException("Не удалось распарсить JSON GraphQL", e);
        }
    }

    private <T> T handleGraphQlErrors(JsonNode errors) {
        throw new GraphQlRemoteException("GraphQL errors: " + errors.toString());
    }

    public record GraphQlRequest(String operationName, Map<String, Object> variables, String query) {}
    public static class GraphQlRemoteException extends RuntimeException {
        public GraphQlRemoteException(String m) { super(m); }
        public GraphQlRemoteException(String m, Throwable c) { super(m, c); }
    }
}
