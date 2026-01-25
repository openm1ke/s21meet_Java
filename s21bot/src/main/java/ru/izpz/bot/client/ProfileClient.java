package ru.izpz.bot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.izpz.dto.*;
import ru.izpz.dto.model.ParticipantV1DTO;

@FeignClient(
    name = "profile",
    url = "${profile.service.url}",
    path = "/profile"
)
public interface ProfileClient {

    @GetMapping
    ProfileDto getOrCreateProfile(@RequestParam String telegramId);

    @PostMapping
    ProfileDto updateProfileStatus(@RequestBody ProfileRequest request);

    @GetMapping("/login")
    ParticipantV1DTO checkEduLogin(@RequestParam String login);

    @PostMapping("/login")
    ProfileDto checkAndSetLogin(@RequestBody ProfileRequest request);

    @PostMapping("/code")
    ProfileCodeResponse getProfileCode(@RequestBody ProfileCodeRequest request);

    @PostMapping("/campus")
    CampusResponse getCampusMap(@RequestBody CampusRequest request);

    @PostMapping("/participant")
    ParticipantDto getParticipant(@RequestBody ParticipantRequest request);

    @PostMapping("/lastcommand")
    ProfileDto setLastCommand(@RequestBody LastCommandRequest request);

    @PostMapping("/friend")
    FriendDto applyFriend(FriendRequest friendRequest);

    @GetMapping("/friends")
    FriendsSliceDto getFriends(@RequestParam String telegramId, @RequestParam int page, @RequestParam int size);

    @GetMapping("/events")
    EventsSliceDto getEvents(@RequestParam String telegramId, @RequestParam int page, @RequestParam int size);

    @GetMapping("/event")
    EventDto getEvent(@RequestParam long id);
}
