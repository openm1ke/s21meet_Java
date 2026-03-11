package ru.izpz.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.izpz.auth.config.TokenControllerConfig;
import ru.izpz.auth.dto.TokenRequest;
import ru.izpz.auth.model.TokenEntity;
import ru.izpz.auth.service.TokenService;

import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
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

        doReturn("generatedToken").when(tokenService).getAccessToken("user1", "pass1");

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedToken"));
    }

    @Test
    void generateToken_returnInternalServerError_whenTokenIsNull() throws Exception {
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
    void getDefaultToken_returnInternalServerError_whenTokenIsNull() throws Exception {
        doReturn(null).when(tokenService).getDefaultAccessToken();
        
        mockMvc.perform(get("/api/tokens/default"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDefaultToken_returnOk_whenTokenExists() throws Exception {
        doReturn("generatedToken").when(tokenService).getDefaultAccessToken();
        
        mockMvc.perform(get("/api/tokens/default"))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedToken"));
    }

    @Test
    void getToken_returnOk_whenTokenExists() throws Exception {
        String login = "user1";
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setLogin(login);
        tokenEntity.setAccessToken("generatedToken");
        tokenEntity.setPassword("pass1");
        tokenEntity.setExpiresAt(null);
        tokenEntity.setRefreshToken(null);
        
        Optional<TokenEntity> token = Optional.of(tokenEntity);
        when(tokenService.findById(login)).thenReturn(token);
        
        mockMvc.perform(get("/api/tokens").param("login", login))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedToken"));
    }

    @Test
    void getToken_returnNotFound_whenTokenNotExists() throws Exception {
        String login = "nonexistent";
        
        when(tokenService.findById(login)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/tokens").param("login", login))
                .andExpect(status().isNotFound());
    }
}