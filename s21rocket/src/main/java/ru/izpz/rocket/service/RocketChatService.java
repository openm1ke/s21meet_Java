package ru.izpz.rocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.rocket.client.RocketChatWebSocketClient;
import ru.izpz.rocket.property.RocketChatProperties;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class RocketChatService {

    private final RocketChatProperties properties;

    RocketChatWebSocketClient createClient(String targetUsername, String messageToSend, boolean isQrMode) {
        return new RocketChatWebSocketClient(
            properties.getWebsocketUri(),
            properties.getToken(),
            targetUsername,
            messageToSend,
            isQrMode
        );
    }

    @PostConstruct
    public void validateConfiguration() {
        log.debug("Validating Rocket.Chat configuration...");
        
        if (!StringUtils.hasText(properties.getWebsocketUri())) {
            log.error("Rocket.Chat WebSocket URL is not configured");
            throw new IllegalStateException("Rocket.Chat WebSocket URL is not configured");
        }
        if (!StringUtils.hasText(properties.getBotUsername())) {
            log.error("Rocket.Chat bot username is not configured");
            throw new IllegalStateException("Rocket.Chat bot username is not configured");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            log.error("Rocket.Chat token is not configured");
            throw new IllegalStateException("Rocket.Chat token is not configured");
        }
        if (!properties.getWebsocketUri().startsWith("ws://") && !properties.getWebsocketUri().startsWith("wss://")) {
            log.error("Invalid Rocket.Chat WebSocket URL format: {}", properties.getWebsocketUri());
            throw new IllegalStateException("Rocket.Chat WebSocket URL must start with ws:// or wss://");
        }
        
        log.info("Rocket.Chat service initialized successfully with URL: {}, bot: {}, QR timeout: {}s, message timeout: {}s", 
            properties.getWebsocketUri(), 
            properties.getBotUsername(),
            properties.getQrTimeout(),
            properties.getMessageTimeout()
        );
    }

    public RocketChatSendResponse generateQrCode() {
        log.info("Starting QR code generation for bot user: {}", properties.getBotUsername());
        
        try {
            log.debug("Creating WebSocket client for QR generation");
            RocketChatWebSocketClient client = createClient(properties.getBotUsername(), null, true);
            
            log.debug("Executing QR generation with timeout: {}s", properties.getQrTimeout());
            RocketChatSendResponse response = client.execute(properties.getQrTimeout());
            
            if (response.isSuccess()) {
                log.info("QR code generated successfully");
            } else {
                log.warn("QR code generation failed: {}", response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Unexpected error during QR code generation", e);
            return new RocketChatSendResponse(false, "Failed to generate QR code: " + e.getMessage());
        }
    }

    public RocketChatSendResponse sendVerificationCode(String targetUsername, String message) {
        log.info("Starting verification code sending to user: {}", targetUsername);
        
        // Input validation
        if (!StringUtils.hasText(targetUsername)) {
            String errorMsg = "Target username cannot be null or empty";
            log.error("Validation failed: {}", errorMsg);
            return new RocketChatSendResponse(false, errorMsg);
        }
        
        if (!StringUtils.hasText(message)) {
            String errorMsg = "Message cannot be null or empty";
            log.error("Validation failed for user {}: {}", targetUsername, errorMsg);
            return new RocketChatSendResponse(false, errorMsg);
        }
        
        try {
            log.debug("Creating WebSocket client for message sending to user: {}", targetUsername);
            RocketChatWebSocketClient client = createClient(targetUsername, message, false);
            
            log.debug("Executing message sending with timeout: {}s", properties.getMessageTimeout());
            RocketChatSendResponse response = client.execute(properties.getMessageTimeout());
            
            if (response.isSuccess()) {
                log.info("Verification code sent successfully to user: {}", targetUsername);
            } else {
                log.warn("Failed to send verification code to user {}: {}", targetUsername, response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Unexpected error during verification code sending to user: {}", targetUsername, e);
            return new RocketChatSendResponse(false, "Failed to send verification code: " + e.getMessage());
        }
    }
}



