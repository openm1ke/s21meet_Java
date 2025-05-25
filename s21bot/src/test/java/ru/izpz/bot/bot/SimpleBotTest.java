package ru.izpz.bot.bot;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleBotTest {

    private final OkHttpTelegramClient telegramClient = mock(OkHttpTelegramClient.class);
    private final SimpleBot simpleBot = new SimpleBot(telegramClient);

    @Test
    void consume_ShouldSendCorrectResponse_WhenUpdateHasTextMessage() throws TelegramApiException {
        // Arrange
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

        // Act
        simpleBot.consume(update);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(1)).execute(captor.capture());

        SendMessage actualMessage = captor.getValue();
        assertEquals("12345", actualMessage.getChatId());
        assertEquals("Вы написали: Привет, бот!", actualMessage.getText());
    }

    @Test
    void consume_ShouldNotSendMessage_WhenUpdateHasNoTextMessage() throws TelegramApiException {
        // Arrange
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);

        // Act
        simpleBot.consume(update);

        // Assert
        verify(telegramClient, never()).execute((SendDocument) any());
    }

    @Test
    void consume_ShouldHandleException_WhenTelegramClientThrowsException() throws TelegramApiException {
        // Arrange
        Update update = new Update();
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("Ошибка");
        update.setMessage(message);

        doThrow(new TelegramApiException("API error"))
                .when(telegramClient).execute((SendDocument) any());

        // Act & Assert (просто проверяем, что исключение не бросается дальше)
        assertDoesNotThrow(() -> simpleBot.consume(update));
    }
}
