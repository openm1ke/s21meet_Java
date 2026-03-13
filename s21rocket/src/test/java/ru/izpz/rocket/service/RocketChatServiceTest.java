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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        properties.setQrTimeout(1L);
        properties.setMessageTimeout(1L);
        
        // Создаем новый сервис с правильными пропертями
        rocketChatService = new RocketChatService(properties);
    }

    @Test
    void generateQrCode_shouldSingleFlight_whenCalledConcurrently() throws Exception {
        // Given
        AtomicInteger executeCalls = new AtomicInteger();
        CountDownLatch bothCallsReady = new CountDownLatch(2);
        CountDownLatch executeStarted = new CountDownLatch(1);
        CountDownLatch allowExecuteFinish = new CountDownLatch(1);

        RocketChatService service = new RocketChatService(properties) {
            @Override
            public RocketChatSendResponse generateQrCode() {
                bothCallsReady.countDown();
                try {
                    if (!bothCallsReady.await(2, TimeUnit.SECONDS)) {
                        return new RocketChatSendResponse(false, "barrier-timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new RocketChatSendResponse(false, "barrier-interrupted");
                }
                return super.generateQrCode();
            }

            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        executeCalls.incrementAndGet();
                        executeStarted.countDown();
                        try {
                            if (!allowExecuteFinish.await(3, TimeUnit.SECONDS)) {
                                return new RocketChatSendResponse(false, "blocked");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return new RocketChatSendResponse(false, "interrupted");
                        }
                        return new RocketChatSendResponse(true, "qr-ok");
                    }
                };
            }
        };

        var executor = Executors.newFixedThreadPool(2);
        try {
            // When
            Future<RocketChatSendResponse> f1 = executor.submit(service::generateQrCode);
            Future<RocketChatSendResponse> f2 = executor.submit(service::generateQrCode);

            assertTrue(executeStarted.await(2, TimeUnit.SECONDS), "execute() should start");
            allowExecuteFinish.countDown();

            RocketChatSendResponse r1 = f1.get(3, TimeUnit.SECONDS);
            RocketChatSendResponse r2 = f2.get(3, TimeUnit.SECONDS);

            // Then
            assertNotNull(r1);
            assertNotNull(r2);
            assertTrue(r1.isSuccess());
            assertTrue(r2.isSuccess());
            assertEquals("qr-ok", r1.getMessage());
            assertEquals("qr-ok", r2.getMessage());
            assertEquals(1, executeCalls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void generateQrCode_shouldAllowNewExecution_afterPreviousCompleted() {
        // Given
        AtomicInteger executeCalls = new AtomicInteger();

        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        int call = executeCalls.incrementAndGet();
                        return new RocketChatSendResponse(true, "qr-ok-" + call);
                    }
                };
            }
        };

        // When
        RocketChatSendResponse r1 = service.generateQrCode();
        RocketChatSendResponse r2 = service.generateQrCode();

        // Then
        assertTrue(r1.isSuccess());
        assertTrue(r2.isSuccess());
        assertEquals("qr-ok-1", r1.getMessage());
        assertEquals("qr-ok-2", r2.getMessage());
        assertEquals(2, executeCalls.get());
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
            default -> throw new IllegalArgumentException("Unknown property: " + propertyToModify);
        }
        rocketChatService = new RocketChatService(properties);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> rocketChatService.validateConfiguration());
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    static java.util.stream.Stream<Arguments> sendVerificationCodeInvalidArguments() {
        return java.util.stream.Stream.of(
                Arguments.of(null, "Test message", "Target username cannot be null or empty"),
                Arguments.of("", "Test message", "Target username cannot be null or empty"),
                Arguments.of("testuser", null, "Message cannot be null or empty"),
                Arguments.of("testuser", "", "Message cannot be null or empty")
        );
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
        // Given
        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        return new RocketChatSendResponse(true, "qr-ok");
                    }
                };
            }
        };

        // When
        RocketChatSendResponse result = service.generateQrCode();

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("qr-ok", result.getMessage());
    }

    @Test
    void generateQrCode_shouldReturnErrorResponse_whenExceptionThrown() {
        // Given
        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                throw new RuntimeException("boom");
            }
        };

        // When
        RocketChatSendResponse result = service.generateQrCode();

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Failed to generate QR code: boom", result.getMessage());
    }

    @Test
    void generateQrCode_shouldReturnFailureResponse_whenClientReturnsFailure() {
        // Given
        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        return new RocketChatSendResponse(false, "qr-failed");
                    }
                };
            }
        };

        // When
        RocketChatSendResponse result = service.generateQrCode();

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("qr-failed", result.getMessage());
    }

    @ParameterizedTest
    @MethodSource("sendVerificationCodeInvalidArguments")
    void sendVerificationCode_shouldReturnErrorResponse_whenArgumentsInvalid(String username, String message, String expectedErrorMessage) {
        // Given
        // When
        RocketChatSendResponse result = rocketChatService.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(expectedErrorMessage, result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldProcessValidRequest() {
        // Given
        String username = "testuser";
        String message = "Test verification code: 123456";

        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        return new RocketChatSendResponse(true, messageToSend);
                    }
                };
            }
        };

        // When
        RocketChatSendResponse result = service.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(message, result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldReturnErrorResponse_whenExceptionThrown() {
        // Given
        String username = "testuser";
        String message = "Test verification code: 123456";

        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                throw new RuntimeException("ws down");
            }
        };

        // When
        RocketChatSendResponse result = service.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Failed to send verification code: ws down", result.getMessage());
    }

    @Test
    void sendVerificationCode_shouldReturnFailureResponse_whenClientReturnsFailure() {
        // Given
        String username = "testuser";
        String message = "Test verification code: 123456";

        RocketChatService service = new RocketChatService(properties) {
            @Override
            RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
                return new RocketChatWebSocketClient(properties.getWebsocketUri(), properties.getToken(), targetUsername, messageToSend, isQrMode) {
                    @Override
                    public RocketChatSendResponse execute(long timeoutSeconds) {
                        return new RocketChatSendResponse(false, "send-failed");
                    }
                };
            }
        };

        // When
        RocketChatSendResponse result = service.sendVerificationCode(username, message);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("send-failed", result.getMessage());
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
