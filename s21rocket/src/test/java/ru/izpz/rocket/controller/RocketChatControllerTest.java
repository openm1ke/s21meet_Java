package ru.izpz.rocket.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.izpz.dto.RocketChatSendRequest;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.rocket.service.RocketChatService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RocketChatControllerTest {

    @Mock
    private RocketChatService rocketChatService;

    @InjectMocks
    private RocketChatController rocketChatController;

    @Test
    void generateQr_shouldReturnResponseFromService() {
        // Given
        RocketChatSendResponse expectedResponse = new RocketChatSendResponse(true, "QR code generated");
        when(rocketChatService.generateQrCode()).thenReturn(expectedResponse);

        // When
        ResponseEntity<RocketChatSendResponse> response = rocketChatController.generateQr();

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(expectedResponse, response.getBody());
        verify(rocketChatService, times(1)).generateQrCode();
    }

    @Test
    void sendMessage_shouldReturnResponseFromService() {
        // Given
        String username = "testuser";
        String message = "Test message";
        RocketChatSendRequest request = new RocketChatSendRequest(username, message);
        RocketChatSendResponse expectedResponse = new RocketChatSendResponse(true, "Message sent");
        when(rocketChatService.sendVerificationCode(username, message)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RocketChatSendResponse> response = rocketChatController.sendMessage(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(expectedResponse, response.getBody());
        verify(rocketChatService, times(1)).sendVerificationCode(username, message);
    }

    @Test
    void sendMessage_shouldHandleNullRequest() {
        // Given
        RocketChatSendResponse expectedResponse = new RocketChatSendResponse(false, "Request body is invalid");

        // When
        ResponseEntity<RocketChatSendResponse> response = rocketChatController.sendMessage(null);

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertEquals(expectedResponse, response.getBody());
        verify(rocketChatService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void sendMessage_shouldHandleValidRequest() {
        // Given
        String username = "testuser";
        String message = "Test message";
        RocketChatSendRequest request = new RocketChatSendRequest(username, message);
        RocketChatSendResponse expectedResponse = new RocketChatSendResponse(true, "Message sent successfully");
        when(rocketChatService.sendVerificationCode(username, message)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RocketChatSendResponse> response = rocketChatController.sendMessage(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(expectedResponse, response.getBody());
        verify(rocketChatService, times(1)).sendVerificationCode(username, message);
    }

    @Test
    void generateQr_shouldHandleServiceException() {
        // Given
        RocketChatSendResponse errorResponse = new RocketChatSendResponse(false, "Service error");
        when(rocketChatService.generateQrCode()).thenReturn(errorResponse);

        // When
        ResponseEntity<RocketChatSendResponse> response = rocketChatController.generateQr();

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(errorResponse, response.getBody());
        verify(rocketChatService, times(1)).generateQrCode();
    }
}
