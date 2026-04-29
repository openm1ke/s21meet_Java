package ru.izpz.auth.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
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

@AutoConfigureMockMvc
@WebMvcTest(TokenController.class)
@Import(TokenControllerConfig.class)
class TokenControllerTest {
  private static final String USER_1 = "user1";
  private static final String PASS_1 = "pass1";
  private static final String GENERATED_TOKEN = "generatedToken";
  private static final String TOKENS_PATH = "/api/tokens";

  @Autowired private MockMvc mockMvc;

  @Autowired private TokenService tokenService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void generateToken_returnsOk_whenTokenIsGenerated() throws Exception {
    TokenRequest request = new TokenRequest();
    request.setLogin(USER_1);
    request.setPassword(PASS_1);

    doReturn(GENERATED_TOKEN).when(tokenService).getAccessToken(USER_1, PASS_1);

    mockMvc
        .perform(
            post(TOKENS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(GENERATED_TOKEN));
  }

  @Test
  void generateToken_returnInternalServerError_whenTokenIsNull() throws Exception {
    TokenRequest request = new TokenRequest();
    request.setLogin(USER_1);
    request.setPassword(PASS_1);

    doReturn(null).when(tokenService).getAccessToken(USER_1, PASS_1);

    mockMvc
        .perform(
            post(TOKENS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void getDefaultToken_returnInternalServerError_whenTokenIsNull() throws Exception {
    doReturn(null).when(tokenService).getDefaultAccessToken();

    mockMvc.perform(get("/api/tokens/default")).andExpect(status().isInternalServerError());
  }

  @Test
  void getDefaultToken_returnOk_whenTokenExists() throws Exception {
    doReturn(GENERATED_TOKEN).when(tokenService).getDefaultAccessToken();

    mockMvc
        .perform(get("/api/tokens/default"))
        .andExpect(status().isOk())
        .andExpect(content().string(GENERATED_TOKEN));
  }

  @Test
  void getToken_returnOk_whenTokenExists() throws Exception {
    String login = USER_1;
    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setLogin(login);
    tokenEntity.setAccessToken(GENERATED_TOKEN);
    tokenEntity.setPassword(PASS_1);
    tokenEntity.setExpiresAt(null);
    tokenEntity.setRefreshToken(null);

    Optional<TokenEntity> token = Optional.of(tokenEntity);
    when(tokenService.findById(login)).thenReturn(token);

    mockMvc
        .perform(get("/api/tokens").param("login", login))
        .andExpect(status().isOk())
        .andExpect(content().string(GENERATED_TOKEN));
  }

  @Test
  void getToken_returnNotFound_whenTokenNotExists() throws Exception {
    String login = "nonexistent";

    when(tokenService.findById(login)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/tokens").param("login", login)).andExpect(status().isNotFound());
  }
}
