package edu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.config.TokenControllerConfig;
import edu.dto.TokenRequest;
import edu.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TokenController.class)
@Import(TokenControllerConfig.class)
class TokenControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateToken_returnsOk_whenTokenIsGenerated() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setLogin("user1");
        request.setPassword("pass1");

        // Подменяем результат без вызова реального метода
        doReturn("generatedToken").when(tokenService).getAccessToken("user1", "pass1");

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedToken"));
    }

    @Test
    void generateToken_returnsInternalServerError_whenTokenIsNull() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setLogin("user1");
        request.setPassword("pass1");

        doReturn(null).when(tokenService).getAccessToken("user1", "pass1");

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generateToken_returnsInternalServerError_whenExceptionThrown() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setLogin("user1");
        request.setPassword("pass1");

        doThrow(new RuntimeException("Test exception"))
                .when(tokenService).getAccessToken("user1", "pass1");

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}