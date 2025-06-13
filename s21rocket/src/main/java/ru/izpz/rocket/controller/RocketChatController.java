package ru.izpz.rocket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.izpz.dto.RocketChatSendRequest;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.rocket.service.RocketChatService;

@RestController
@RequestMapping("/api/rocketchat")
@RequiredArgsConstructor
public class RocketChatController {

    private final RocketChatService rocketChatService;

    @PostMapping("/qr")
    public ResponseEntity<RocketChatSendResponse> generateQr() {
        return ResponseEntity.ok(rocketChatService.generateQrCode());
    }

    @PostMapping("/send")
    public ResponseEntity<RocketChatSendResponse> sendMessage(@RequestBody RocketChatSendRequest request) {
        return ResponseEntity.ok(rocketChatService.sendVerificationCode(request.getUsername(), request.getMessage()));
    }
}

