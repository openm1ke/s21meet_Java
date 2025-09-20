package ru.izpz.bot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.izpz.bot.service.MessageSender;
import ru.izpz.dto.NotifyRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotifyController {

    private final MessageSender messageSender;

    @RequestMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody NotifyRequest req) {
        if (req == null || req.getChanges() == null || req.getChanges().isEmpty()) {
            return ResponseEntity.accepted().build();
        }

        messageSender.sendStatusChanges(req.getChanges());
        return ResponseEntity.accepted().build();
    }

}
