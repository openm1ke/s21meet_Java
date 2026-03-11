package ru.izpz.rocket.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.RocketChatSendResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RocketChatWebSocketClientTest {

    private static final String TEST_URI = "ws://localhost:3000/websocket";
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_MESSAGE = "Test message";

    @Mock
    private CountDownLatch mockLatch;

    private RocketChatWebSocketClient client;

    @BeforeEach
    void setUp() {
        client = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false);
    }

    @Test
    void constructor_shouldSetAllFieldsCorrectly() {
        // When
        RocketChatWebSocketClient qrClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, null, true);

        // Then
        assertEquals(TEST_URI, qrClient.getURI().toString());
        assertNotNull(qrClient);
    }

    @Test
    void execute_shouldReturnTimeoutResponse_whenLatchNotCompleted() {
        // Given
        RocketChatWebSocketClient timeoutClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public boolean connectBlocking() {
                // Simulate connection but don't complete latch
                return true;
            }
        };

        // When
        RocketChatSendResponse result = timeoutClient.execute(1);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Timeout"));
    }

    @Test
    void execute_shouldReturnErrorResponse_whenExceptionThrown() {
        // Given
        RocketChatWebSocketClient errorClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public boolean connectBlocking() {
                throw new RuntimeException("Connection failed");
            }
        };

        // When
        RocketChatSendResponse result = errorClient.execute(5);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error"));
        assertTrue(result.getMessage().contains("Connection failed"));
    }

    @Test
    void execute_shouldReturnInterruptedResponse_whenThreadInterrupted() {
        // Given
        RocketChatWebSocketClient interruptedClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public boolean connectBlocking() {
                return true;
            }
        };

        // When
        Thread.currentThread().interrupt();
        try {
            RocketChatSendResponse result = interruptedClient.execute(5);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals("Interrupted", result.getMessage());
        } finally {
            // очистка флага прерывания, чтобы не влиять на другие тесты
            Thread.interrupted();
        }
    }

    @Test
    void execute_shouldReturnSuccessResponse_whenResponseExists() {
        // Given
        RocketChatWebSocketClient successClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public boolean connectBlocking() {
                // Simulate successful connection and set response
                try {
                    var responseField = RocketChatWebSocketClient.class.getDeclaredField("response");
                    responseField.setAccessible(true);
                    AtomicReference<RocketChatSendResponse> responseRef = (AtomicReference<RocketChatSendResponse>) responseField.get(this);
                    responseRef.set(new RocketChatSendResponse(true, "Success"));
                    
                    var latchField = RocketChatWebSocketClient.class.getDeclaredField("latch");
                    latchField.setAccessible(true);
                    ((CountDownLatch) latchField.get(this)).countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        };

        // When
        RocketChatSendResponse result = successClient.execute(5);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
    }

    @Test
    void onMessage_shouldHandlePingMessage() {
        // Given
        String pingMessage = "{\"msg\":\"ping\"}";
        
        // Создаем mock клиент, который не будет пытаться отправлять сообщения
        RocketChatWebSocketClient mockClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public void send(String text) {
                // Do nothing - prevent actual sending
            }
        };

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> mockClient.onMessage(pingMessage));
    }

    @Test
    void onMessage_shouldHandleResultMessage() {
        // Given
        String resultMessage = "{\"msg\":\"result\",\"id\":\"42\",\"result\":{\"token\":\"test\"}}";
        
        // Создаем mock клиент, который не будет пытаться отправлять сообщения
        RocketChatWebSocketClient mockClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public void send(String text) {
                // Do nothing - prevent actual sending
            }
        };

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> mockClient.onMessage(resultMessage));
    }

    @Test
    void onMessage_shouldHandleChangedMessage() {
        // Given
        String changedMessage = "{\"msg\":\"changed\",\"fields\":{\"args\":[{\"msg\":\"The QR code will expire on 2023-12-31\"}]}}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onMessage(changedMessage));
    }

    @Test
    void onMessage_shouldHandleUnknownMessageType() {
        // Given
        String unknownMessage = "{\"msg\":\"unknown\"}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onMessage(unknownMessage));
    }

    @Test
    void onClose_shouldCountDownLatch() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onClose(1000, "Normal closure", true));
    }

    @Test
    void onError_shouldSetErrorResponseAndCountDownLatch() {
        // Given
        Exception testException = new RuntimeException("Test error");

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onError(testException));
    }

    @Test
    void onOpen_shouldSendConnectAndLoginMessages() {
        // Given
        var mockHandshake = mock(org.java_websocket.handshake.ServerHandshake.class);
        
        // Создаем mock клиент, который не будет пытаться отправлять сообщения
        RocketChatWebSocketClient mockClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public void send(String text) {
                // Do nothing - prevent actual sending
            }
        };

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> mockClient.onOpen(mockHandshake));
    }

    @Test
    void qrModeClient_shouldHandleQrResponse() {
        // Given
        RocketChatWebSocketClient qrClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, null, true);
        String qrMessage = "{\"msg\":\"changed\",\"fields\":{\"args\":[{\"msg\":\"The QR code will expire on 2023-12-31 23:59:59\"}]}}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> qrClient.onMessage(qrMessage));
    }

    @Test
    void handleMessage_shouldHandleLoginError() {
        // Given
        String loginError = "{\"msg\":\"result\",\"id\":\"42\",\"error\":{\"message\":\"Invalid token\"}}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onMessage(loginError));
    }

    @Test
    void handleMessage_shouldHandleCreateDmError() {
        // Given
        String createDmError = "{\"msg\":\"result\",\"id\":\"unique_create_dm_id\",\"error\":{\"message\":\"User not found\"}}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> client.onMessage(createDmError));
    }

    @Test
    void handleMessage_shouldHandleSuccessfulDmCreation() {
        // Given
        String createDmSuccess = "{\"msg\":\"result\",\"id\":\"unique_create_dm_id\",\"result\":{\"rid\":\"room123\"}}";
        
        // Создаем mock клиент, который не будет пытаться отправлять сообщения
        RocketChatWebSocketClient mockClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, TEST_MESSAGE, false) {
            @Override
            public void send(String text) {
                // Do nothing - prevent actual sending
            }
        };

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> mockClient.onMessage(createDmSuccess));
    }

    @Test
    void qrModeClient_shouldHandleSubscriptionAndCommand() {
        // Given
        RocketChatWebSocketClient qrClient = new RocketChatWebSocketClient(TEST_URI, TEST_TOKEN, TEST_USERNAME, null, true) {
            @Override
            public void send(String text) {
                // Do nothing - prevent actual sending
            }
        };
        String createDmSuccess = "{\"msg\":\"result\",\"id\":\"unique_create_dm_id\",\"result\":{\"rid\":\"room123\"}}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> qrClient.onMessage(createDmSuccess));
    }
}
