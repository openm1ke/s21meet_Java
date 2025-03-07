package edu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dto.TokenRequest;
import edu.model.TokenEntity;
import edu.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TokenController.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateToken_returnsOk_whenTokenIsGenerated() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setLogin("user1");
        request.setPassword("pass1");

        when(tokenService.getAccessToken("user1", "pass1")).thenReturn("generatedToken");

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

        when(tokenService.getAccessToken("user1", "pass1")).thenReturn(null);

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

        when(tokenService.getAccessToken("user1", "pass1")).thenThrow(new RuntimeException("Test exception"));

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDefaultToken_returnsOk_whenTokenIsGenerated() throws Exception {
        when(tokenService.getDefaultAccessToken()).thenReturn("defaultToken");

        mockMvc.perform(get("/api/tokens/default"))
                .andExpect(status().isOk())
                .andExpect(content().string("defaultToken"));
    }

    @Test
    void getDefaultToken_returnsInternalServerError_whenTokenIsNull() throws Exception {
        when(tokenService.getDefaultAccessToken()).thenReturn(null);

        mockMvc.perform(get("/api/tokens/default"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getToken_returnsOk_whenTokenFound() throws Exception {
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setAccessToken("foundToken");

        when(tokenService.findByLogin("user2")).thenReturn(Optional.of(tokenEntity));

        mockMvc.perform(get("/api/tokens")
                        .param("login", "user2"))
                .andExpect(status().isOk())
                .andExpect(content().string("foundToken"));
    }

    @Test
    void getToken_returnsNotFound_whenTokenNotFound() throws Exception {
        when(tokenService.findByLogin("user2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tokens")
                        .param("login", "user2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getToken_returnsInternalServerError_whenExceptionThrown() throws Exception {
        when(tokenService.findByLogin(anyString())).thenThrow(new RuntimeException("Test exception"));

        mockMvc.perform(get("/api/tokens")
                        .param("login", "user2"))
                .andExpect(status().isInternalServerError());
    }
}