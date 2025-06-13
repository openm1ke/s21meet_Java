package ru.izpz.bot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.izpz.dto.ProfileCodeRequest;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.model.ParticipantV1DTO;

@FeignClient(
    name = "profile",
    url = "${profile.service.url}"
)
public interface ProfileClient {

    @GetMapping("/profile")
    ProfileDto getOrCreateProfile(@RequestParam String telegramId);

    @PostMapping("/profile")
    ProfileDto updateProfileStatus(@RequestBody ProfileRequest request);

    @GetMapping("/profile/login")
    ParticipantV1DTO checkEduLogin(@RequestParam String login);

    @PostMapping("/profile/login")
    ProfileDto checkAndSetLogin(@RequestBody ProfileRequest request);

    @PostMapping("/profile/code")
    ProfileCodeResponse getProfileCode(@RequestBody ProfileCodeRequest request);

}
