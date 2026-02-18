package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramClientProxyTest {

    @Test
    void execute_delegatesToOkHttpTelegramClient() throws TelegramApiException {
        OkHttpTelegramClient delegate = mock(OkHttpTelegramClient.class);
        TelegramClientProxy proxy = new TelegramClientProxy(delegate);

        SendMessage method = SendMessage.builder().chatId("1").text("t").build();
        Message msg = mock(Message.class);
        when(delegate.execute(any(SendMessage.class))).thenReturn(msg);

        Object result = proxy.execute(method);

        assertEquals(msg, result);
        verify(delegate).execute(method);
    }
}
