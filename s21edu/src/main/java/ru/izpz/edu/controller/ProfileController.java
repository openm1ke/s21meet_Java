package ru.izpz.edu.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.izpz.dto.*;
import ru.izpz.dto.CampusDto;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.FriendService;
import ru.izpz.edu.service.ProfileService;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile")
@ConditionalOnProperty(name = "profile.api.enabled", havingValue = "true")
public class ProfileController {

    private final ProfileService profileService;
    private final CampusService campusService;
    private final FriendService friendsService;
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(
            @RequestParam("telegramId")
            @NotBlank
            @Pattern(regexp = "^\\d{5,13}$", message = "Telegram ID должен содержать только цифры и быть длиной от 5 до 13 символов")
            String telegramId) {
        log.info("Получен запрос на вывод профиля для telegramId = {}", telegramId);
        var profile = profileService.getOrCreateProfile(telegramId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<ProfileDto> updateProfileStatus(@Valid @RequestBody ProfileRequest request) {
        log.info("Получен запрос на обновление профиля для telegramId = {}", request.getTelegramId());
        var updateProfile = profileService.updateProfileStatus(request);
        return ResponseEntity.ok(updateProfile);
    }

    @GetMapping("/login")
    ResponseEntity<ParticipantDto> checkEduLogin(@RequestParam @NotBlank String login) throws ApiException {
        log.info("Получен запрос на проверку логина: login = {}", login);
        var participant = profileService.checkEduLogin(login);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/login")
    ResponseEntity<ProfileDto> checkAndSetLogin(@Valid @RequestBody ProfileRequest request) {
        log.info("Получен запрос на привязку логина: login = {} для телеграма = {}", request.getS21login(), request.getTelegramId());
        var profile = profileService.checkAndSetLogin(request.getTelegramId(), request.getS21login());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/code")
    ResponseEntity<ProfileCodeResponse> sendVerificationCode(@Valid @RequestBody ProfileCodeRequest request) {
        log.info("Получен запрос на генерацию кода подтверждения для {}", request.getS21login());
        var verificationCode = profileService.getVerificationCode(request.getS21login());
        return ResponseEntity.ok(verificationCode);
    }

    @PostMapping("/campus")
    ResponseEntity<CampusResponse> getCampus(@Valid @RequestBody CampusRequest request) {
        log.info("Получен запрос на вывод карты кампуса для {}", request.getTelegramId());
        CampusDto campus = profileService.getCampus(request.getTelegramId());
        var snapshot = campusService.getCampusSnapshot(campus);
        var response = CampusResponse.builder()
                .campusName(campus.name)
                .clusters(snapshot.clusters())
                .programStats(snapshot.programStats())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participant")
    ResponseEntity<ParticipantDto> getParticipant(@Valid @RequestBody ParticipantRequest request) throws ApiException {
        log.info("Получен запрос на вывод данных участника {} для {}", request.getEduLogin(), request.getTelegramId());
        var participant = profileService.getParticipant(request.getEduLogin());
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/lastcommand")
    ResponseEntity<ProfileDto> setLastCommand(@Valid @RequestBody LastCommandRequest request) {
        log.info("Получен запрос на обновление последней команды для telegramId = {}, команда {}", request.getTelegramId(), request.getCommand());
        var profile = profileService.updateLastCommand(request);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/friend")
    ResponseEntity<FriendDto> addFriend(@Valid @RequestBody FriendRequest friendRequest) {
        var profile = profileService.getProfile(friendRequest.getTelegramId());
        log.info("Получен запрос на {} у {} для {}", friendRequest.getAction(), friendRequest.getLogin(), profile.s21login());
        var dto = friendsService.applyFriend(profile.telegramId(), friendRequest.getLogin(), friendRequest.getAction(), friendRequest.getName());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/friends")
    ResponseEntity<FriendsSliceDto> getFriends(
            @RequestParam @NotBlank String telegramId,
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size) {
        log.info("Получен запрос на вывод друзей для telegramId = {}, page = {}, size = {}", telegramId, page, size);
        return ResponseEntity.ok(friendsService.getFriends(telegramId, page, size));
    }

    @GetMapping("/event")
    ResponseEntity<EventDto> getEvent(@NotNull @RequestParam Long id) {
        log.info("Получен запрос на вывод события для id = {}", id);
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @GetMapping("/events")
    ResponseEntity<EventsSliceDto> getEvents(
            @RequestParam @NotBlank String telegramId,
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size) {
        log.info("Получен запрос на вывод списка событий кампуса для telegramId = {}, page = {}, size = {}", telegramId, page, size);
        return ResponseEntity.ok(eventService.getEvents(page, size));
    }

    @GetMapping("/projects")
    ResponseEntity<List<ProjectsDto>> getProjects(@RequestParam @NotBlank String login) {
        log.info("Получен запрос на вывод списка провектов для {}", login);
        return ResponseEntity.ok(campusService.getStudentProjectsByLogin(login));
    }
}
