package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private TelegramClientProxy telegramClientProxy;

    @InjectMocks
    private MessageSender messageSender;

    @Test
    void execute_success_returnsOptionalWithValue() throws TelegramApiException {
        Message msg = mock(Message.class);
        when(telegramClientProxy.execute(any())).thenReturn(msg);

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isPresent());
        assertEquals(msg, result.get());
    }

    @Test
    void execute_telegramException_returnsEmptyOptional() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenThrow(new TelegramApiException("err"));

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isEmpty());
    }

    @Test
    void sendMessage_buildsSendMessageAndDelegatesToExecute() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        ReplyKeyboard kb = mock(ReplyKeyboard.class);
        messageSender.sendMessage(1L, "txt", kb);

        verify(telegramClientProxy).execute(any(SendMessage.class));
    }
}
