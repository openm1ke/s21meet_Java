package ru.izpz.rocket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource("validateConfigurationTestCases")
    void validateConfiguration_shouldThrowException_whenPropertiesInvalid(String propertyToModify, Object newValue, String expectedErrorMessage) {
        // Given
        switch (propertyToModify) {
            case "websocketUri" -> properties.setWebsocketUri((String) newValue);
            case "botUsername" -> properties.setBotUsername((String) newValue);
            case "token" -> properties.setToken((String) newValue);
        }
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    static java.util.stream.Stream<Arguments> validateConfigurationTestCases() {
        return java.util.stream.Stream.of(
            Arguments.of("websocketUri", "", "Rocket.Chat WebSocket URL is not configured"),
            Arguments.of("botUsername", null, "Rocket.Chat bot username is not configured"),
            Arguments.of("token", "", "Rocket.Chat token is not configured"),
            Arguments.of("websocketUri", "http://invalid-url", "Rocket.Chat WebSocket URL must start with ws:// or wss://")
        );
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
