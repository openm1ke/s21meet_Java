package ru.izpz.bot.bot;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.service.MessageProcessor;

import static org.mockito.Mockito.*;

class SimpleBotTest {

    private final MessageProcessor messageProcessor = mock(MessageProcessor.class);
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

        verify(messageProcessor, times(1)).handleTextMessage(captor.capture());
    }

    @Test
    void consume_ShouldNotSendMessage_WhenUpdateHasNoTextMessage() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);

        simpleBot.consume(update);

        verifyNoInteractions(messageProcessor);
    }

    @Test
    void consume_WhenChatNotPrivate_doesNothing() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getChat()).thenReturn(chat);
        when(chat.getType()).thenReturn("group");

        simpleBot.consume(update);

        verifyNoInteractions(messageProcessor);
    }

    @Test
    void consume_WhenHasCallbackQuery_delegatesToMessageProcessor() {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getData()).thenReturn("data");
        when(callbackQuery.getId()).thenReturn("cb");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(10L);
        when(message.getMessageId()).thenReturn(5);

        simpleBot.consume(update);

        verify(messageProcessor).handleCallbackMessage(10L, "data", 5, "cb");
    }
}
