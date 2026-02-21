package ru.izpz.rocket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.rocket.client.RocketChatWebSocketClient;
import ru.izpz.rocket.property.RocketChatProperties;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RocketChatServiceTest {

    @Mock
    private RocketChatWebSocketClient webSocketClient;

    @InjectMocks
    private RocketChatService rocketChatService;

    private RocketChatProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RocketChatProperties();
        properties.setWebsocketUri("ws://localhost:3000/websocket");
        properties.setBotUsername("botuser");
        properties.setToken("test-token");
        properties.setQrTimeout(30L);
        properties.setMessageTimeout(15L);
        
        // Создаем новый сервис с правильными пропертями
        rocketChatService = new RocketChatService(properties);
    }

    @Test
    void validateConfiguration_shouldPass_whenAllPropertiesValid() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> rocketChatService.validateConfiguration());
    }

    @Test
    void validateConfiguration_shouldThrowException_whenWebSocketUrlMissing() {
        // Given
        properties.setWebsocketUri("");
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals("Rocket.Chat WebSocket URL is not configured", exception.getMessage());
    }

    @Test
    void validateConfiguration_shouldThrowException_whenBotUsernameMissing() {
        // Given
        properties.setBotUsername(null);
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals("Rocket.Chat bot username is not configured", exception.getMessage());
    }

    @Test
    void validateConfiguration_shouldThrowException_whenTokenMissing() {
        // Given
        properties.setToken("");
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals("Rocket.Chat token is not configured", exception.getMessage());
    }

    @Test
    void validateConfiguration_shouldThrowException_whenWebSocketUrlInvalid() {
        // Given
        properties.setWebsocketUri("http://invalid-url");
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals("Rocket.Chat WebSocket URL must start with ws:// or wss://", exception.getMessage());
    }

    @Test
    void generateQrCode_shouldReturnSuccessResponse() {
        // Given & When
        RocketChatSendResponse result = rocketChatService.generateQrCode();

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessage());
        // В реальном сценарии здесь будет проверка на успешное подключение или ошибку
    }

    @Test
    void sendVerificationCode_shouldReturnErrorResponse_whenTargetUsernameNull() {
        // Given
        String username = null;
        String message = "Test message";

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Target username cannot be null or empty", result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldReturnErrorResponse_whenTargetUsernameEmpty() {
        // Given
        String username = "";
        String message = "Test message";

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Target username cannot be null or empty", result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldReturnErrorResponse_whenMessageNull() {
        // Given
        String username = "testuser";
        String message = null;

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Message cannot be null or empty", result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldReturnErrorResponse_whenMessageEmpty() {
        // Given
        String username = "testuser";
        String message = "";

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Message cannot be null or empty", result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldProcessValidRequest() {
        // Given
        String username = "testuser";
        String message = "Test verification code: 123456";

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessage());
        // В реальном сценарии здесь будет проверка на успешное подключение или ошибку
    }

    @Test
    void sendVerificationCode_shouldHandleWhitespaceOnlyParameters() {
        // Given
        String username = "   ";
        String message = "   ";

        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Target username cannot be null or empty", result.getMessage());
    }
}
