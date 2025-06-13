package ru.izpz.bot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.izpz.dto.RocketChatSendRequest;
import ru.izpz.dto.RocketChatSendResponse;

@FeignClient(
        name = "rocketchat",
        url = "${rocketchat.service.url}"
)
public interface RocketChatClient {
    @PostMapping("/api/rocketchat/send")
    RocketChatSendResponse sendMessage(@RequestBody RocketChatSendRequest request);

    @PostMapping("/api/rocketchat/qr")
    RocketChatSendResponse generateQr();
}
