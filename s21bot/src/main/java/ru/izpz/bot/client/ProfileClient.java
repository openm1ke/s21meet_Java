package ru.izpz.bot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.izpz.bot.dto.ProfileDto;
import ru.izpz.bot.dto.ProfileRequest;

@FeignClient(
    name = "profile",
    url = "${profile.service.url}"
)
public interface ProfileClient {

    @PostMapping("/profile")
    ProfileDto getOrCreateProfile(@RequestBody ProfileRequest request);
}
