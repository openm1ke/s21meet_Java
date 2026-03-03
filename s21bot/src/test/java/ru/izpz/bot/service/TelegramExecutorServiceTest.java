package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramExecutorServiceTest {

    @Test
    void execute_success_returnsOptionalWithValue() throws TelegramApiException {
        TelegramClientProxy telegramClientProxy = mock(TelegramClientProxy.class);
        TelegramExecutorService executorService = new TelegramExecutorService(telegramClientProxy);
        Message msg = mock(Message.class);
        when(telegramClientProxy.execute(any(SendMessage.class))).thenReturn(msg);

        Optional<Message> result = executorService.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isPresent());
        assertEquals(msg, result.get());
    }

    @Test
    void execute_telegramException_returnsEmptyOptional() throws TelegramApiException {
        TelegramClientProxy telegramClientProxy = mock(TelegramClientProxy.class);
        TelegramExecutorService executorService = new TelegramExecutorService(telegramClientProxy);
        when(telegramClientProxy.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("err"));

        Optional<Message> result = executorService.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isEmpty());
        verify(telegramClientProxy).execute(any(SendMessage.class));
    }
}
