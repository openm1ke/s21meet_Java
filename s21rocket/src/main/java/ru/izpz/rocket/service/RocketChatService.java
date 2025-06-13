package ru.izpz.rocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.rocket.client.RocketChatWebSocketClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class RocketChatService {

    @Value("${rocketchat.websocket-uri}")
    private String webSocketUrl;

    @Value("${rocketchat.bot-username}")
    private String botUserName;

    @Value("${rocketchat.token}")
    private String token;

    public RocketChatSendResponse generateQrCode() {
        RocketChatWebSocketClient client = new RocketChatWebSocketClient(webSocketUrl, token, botUserName, null, true);
        return client.execute(30);
    }

    public RocketChatSendResponse sendVerificationCode(String targetUsername, String message) {
        RocketChatWebSocketClient client = new RocketChatWebSocketClient(webSocketUrl, token, targetUsername, message, false);
        return client.execute(15);
    }
}



