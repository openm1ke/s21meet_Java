package ru.izpz.edu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@ConditionalOnProperty(name = "graphql.api.enabled", havingValue = "true")
@RequiredArgsConstructor
public class GraphQLApiClient {

    private static final String GRAPHQL_URL = "https://platform.21-school.ru/services/graphql";
    private static final String SCHOOL_ID = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    private final ObjectMapper om;

    public <T> T execute(String operationName,
                         Map<String, Object> variables,
                         String query,
                         Class<T> dataClass) {

        String token = tokenService.getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.add("schoolId", SCHOOL_ID);

        GraphQlRequest body = new GraphQlRequest(operationName, variables, query);
        HttpEntity<GraphQlRequest> entity = new HttpEntity<>(body, headers);
        //System.out.println("GraphQL request: " + om.valueToTree(body));
        ResponseEntity<String> resp;
        try {
            resp = restTemplate.postForEntity(GRAPHQL_URL, entity, String.class);
        } catch (HttpStatusCodeException e) {
            throw new GraphQlRemoteException("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }

        String raw = resp.getBody();
        if (raw == null || raw.isBlank()) {
            throw new GraphQlRemoteException("Пустой ответ GraphQL");
        }

        try {
            JsonNode root = om.readTree(raw);
            if (root.has("errors") && root.get("errors").isArray() && !root.get("errors").isEmpty()) {
                return handleGraphQlErrors(root.get("errors"));
            }
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                throw new GraphQlRemoteException("В ответе нет поля 'data'");
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
