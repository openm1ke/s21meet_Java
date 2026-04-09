package ru.izpz.edu.client;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ApiResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
class PlatformApiClientTest {

    @Test
    void execute_withoutReturnType_returnsStatusCode() throws Exception {
        PlatformApiClient apiClient = new PlatformApiClient(new OkHttpClient());
        ApiResponse<Void> response = apiClient.execute(mockOkCall200());

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void execute_withReturnType_returnsStatusCode() throws Exception {
        PlatformApiClient apiClient = new PlatformApiClient(new OkHttpClient());
        ApiResponse<Void> response = apiClient.execute(mockOkCall200(), null);

        assertEquals(200, response.getStatusCode());
    }

    private static Call mockOkCall200() throws IOException {
        Call call = mock(Call.class);
        Response response = new Response.Builder()
            .request(new Request.Builder().url("http://localhost/ok").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("{}", MediaType.get("application/json")))
            .build();
        when(call.execute()).thenReturn(response);
        return call;
    }
}
