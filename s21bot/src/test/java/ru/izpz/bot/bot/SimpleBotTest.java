package ru.izpz.bot.bot;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.bot.service.MessageProcessor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SimpleBotTest {

    private final MessageProcessor messageProcessor = mock(MessageProcessor.class);
    private final OkHttpTelegramClient telegramClient = mock(OkHttpTelegramClient.class);
    private final SimpleBot simpleBot = new SimpleBot(messageProcessor);

    @Test
    void consume_ShouldSendCorrectResponse_WhenUpdateHasTextMessage() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getType()).thenReturn("private");
        when(message.getText()).thenReturn("Привет, бот!");

        simpleBot.consume(update);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        verify(messageProcessor, times(1)).process(captor.capture());
    }

    @Test
    void consume_ShouldNotSendMessage_WhenUpdateHasNoTextMessage() throws TelegramApiException {
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);

        simpleBot.consume(update);

        verify(telegramClient, never()).execute((SendDocument) any());
    }

    @Test
    void consume_ShouldHandleException_WhenTelegramClientThrowsException() throws TelegramApiException {
        Update update = new Update();
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("Ошибка");
        update.setMessage(message);

        doThrow(new TelegramApiException("API error"))
                .when(telegramClient).execute((SendDocument) any());

        assertDoesNotThrow(() -> simpleBot.consume(update));
    }
}
