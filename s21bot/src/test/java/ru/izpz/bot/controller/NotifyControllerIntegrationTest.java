package ru.izpz.bot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.mockito.ArgumentCaptor;
import ru.izpz.bot.service.TelegramClientProxy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "bot.notify.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotifyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramClientProxy telegramClientProxy;

    @Test
    void notify_shouldSendStatusMessagesViaTelegramApi_forEachValidTelegramId() throws Exception {
        Message message = mock(Message.class);
        doReturn(message).when(telegramClientProxy).execute(any(SendMessage.class));

        mockMvc.perform(post("/api/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClientProxy, times(2)).execute(captor.capture());

        List<SendMessage> methods = captor.getAllValues();
        SendMessage first = assertInstanceOf(SendMessage.class, methods.get(0));
        SendMessage second = assertInstanceOf(SendMessage.class, methods.get(1));

        assertEquals("1001", first.getChatId());
        assertEquals("alice is online", first.getText());
        assertEquals("1002", second.getChatId());
        assertEquals("alice is online", second.getText());
    }

    @Test
    void notify_shouldSkipInvalidTelegramIds_andSendOnlyForValidOnes() throws Exception {
        Message message = mock(Message.class);
        doReturn(message).when(telegramClientProxy).execute(any(SendMessage.class));

        mockMvc.perform(post("/api/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClientProxy, times(2)).execute(captor.capture());

        List<SendMessage> methods = captor.getAllValues();
        assertEquals("1001", methods.get(0).getChatId());
        assertEquals("bob is online", methods.get(0).getText());
        assertEquals("1002", methods.get(1).getChatId());
        assertEquals("bob is online", methods.get(1).getText());
    }

    @Test
    void notify_shouldSendOfflineMessage_whenStatusIsFalse() throws Exception {
        Message message = mock(Message.class);
        doReturn(message).when(telegramClientProxy).execute(any(SendMessage.class));

        mockMvc.perform(post("/api/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClientProxy, times(1)).execute(captor.capture());

        SendMessage sent = captor.getValue();
        assertEquals("2001", sent.getChatId());
        assertEquals("alice is offline", sent.getText());
    }
}
