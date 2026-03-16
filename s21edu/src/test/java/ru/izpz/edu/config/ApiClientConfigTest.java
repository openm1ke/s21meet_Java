package ru.izpz.edu.config;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.service.TokenService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiClientConfigTest {

    private final ApiClientConfig config = new ApiClientConfig();

    @Test
    void apiClient_shouldBuildClientWithAuthInterceptor() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        when(tokenService.getToken()).thenReturn("test-token");

        ApiClient apiClient = config.apiClient(tokenService);

        assertNotNull(apiClient);
        assertNotNull(apiClient.getHttpClient());
        assertEquals(1, apiClient.getHttpClient().interceptors().size());

        Request original = new Request.Builder().url("http://localhost/test").build();
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(original);
        doAnswer(invocation -> {
            Request processed = invocation.getArgument(0);
            return new Response.Builder()
                    .request(processed)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create("", MediaType.get("text/plain")))
                    .build();
        }).when(chain).proceed(any(Request.class));

        Response response = assertDoesNotThrow(() ->
                apiClient.getHttpClient().interceptors().get(0).intercept(chain)
        );

        assertNotNull(response);
        assertEquals("Bearer test-token", response.request().header("Authorization"));
        verify(tokenService, times(1)).getToken();
    }

    @Test
    void restTemplate_shouldCreateNewInstance() {
        RestTemplate restTemplate = config.restTemplate();

        assertNotNull(restTemplate);
    }

    @Test
    void apiBeans_shouldUseProvidedApiClient() {
        ApiClient apiClient = new ApiClient(new OkHttpClient());

        CampusApi campusApi = config.campusApi(apiClient);
        ClusterApi clusterApi = config.clusterApi(apiClient);
        ParticipantApi participantApi = config.participantApi(apiClient);
        EventApi eventApi = config.eventApi(apiClient);

        assertSame(apiClient, campusApi.getApiClient());
        assertSame(apiClient, clusterApi.getApiClient());
        assertSame(apiClient, participantApi.getApiClient());
        assertSame(apiClient, eventApi.getApiClient());
    }
}
