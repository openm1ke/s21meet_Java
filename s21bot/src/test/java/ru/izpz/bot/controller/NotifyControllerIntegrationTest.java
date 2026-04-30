package ru.izpz.bot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.izpz.bot.service.MessageSender;
import ru.izpz.dto.StatusChange;

@ExtendWith(MockitoExtension.class)
class NotifyControllerIntegrationTest {

  private MockMvc mockMvc;

  @Mock private MessageSender messageSender;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new NotifyController(messageSender)).build();
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<List<StatusChange>> statusChangesCaptor() {
    return (ArgumentCaptor<List<StatusChange>>)
        (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
  }

  @Test
  void notify_shouldSendStatusMessagesViaTelegramApi_forEachValidTelegramId() throws Exception {
    mockMvc
        .perform(
            post("/api/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "changes": [
                                    {
                                      "login": "alice",
                                      "newStatus": true,
                                      "telegramIds": ["1001", "1002"]
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isAccepted());

    ArgumentCaptor<List<StatusChange>> captor = statusChangesCaptor();
    verify(messageSender).sendStatusChanges(captor.capture());
    List<StatusChange> changes = captor.getValue();
    assertEquals(1, changes.size());
    assertEquals("alice", changes.getFirst().login());
    assertTrue(changes.getFirst().newStatus());
    assertEquals(List.of("1001", "1002"), changes.getFirst().telegramIds());
  }

  @Test
  void notify_shouldSkipInvalidTelegramIds_andSendOnlyForValidOnes() throws Exception {
    mockMvc
        .perform(
            post("/api/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "changes": [
                                    {
                                      "login": "bob",
                                      "newStatus": true,
                                      "telegramIds": ["1001", "bad-id", "1002"]
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isAccepted());

    ArgumentCaptor<List<StatusChange>> captor = statusChangesCaptor();
    verify(messageSender).sendStatusChanges(captor.capture());
    List<StatusChange> changes = captor.getValue();
    assertEquals(1, changes.size());
    assertEquals("bob", changes.getFirst().login());
    assertTrue(changes.getFirst().newStatus());
    assertEquals(List.of("1001", "bad-id", "1002"), changes.getFirst().telegramIds());
  }

  @Test
  void notify_shouldSendOfflineMessage_whenStatusIsFalse() throws Exception {
    mockMvc
        .perform(
            post("/api/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "changes": [
                                    {
                                      "login": "alice",
                                      "newStatus": false,
                                      "telegramIds": ["2001"]
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isAccepted());

    ArgumentCaptor<List<StatusChange>> captor = statusChangesCaptor();
    verify(messageSender).sendStatusChanges(captor.capture());
    List<StatusChange> changes = captor.getValue();
    assertEquals(1, changes.size());
    assertEquals("alice", changes.getFirst().login());
    assertFalse(changes.getFirst().newStatus());
    assertEquals(List.of("2001"), changes.getFirst().telegramIds());
  }

  @Test
  void notify_shouldNotInvokeSender_whenBodyHasNoChanges() throws Exception {
    mockMvc
        .perform(
            post("/api/notify").contentType(MediaType.APPLICATION_JSON).content("{\"changes\":[]}"))
        .andExpect(status().isAccepted());

    verify(messageSender, never()).sendStatusChanges(any());
  }
}
