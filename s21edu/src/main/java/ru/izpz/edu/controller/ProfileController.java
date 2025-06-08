package ru.izpz.edu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.ProfileService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
@ConditionalOnProperty(name = "profile.api.enabled", havingValue = "true", matchIfMissing = true)
public class ProfileController {

    private final ProfileService profileService;
    private final CampusService campusService;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@RequestParam("telegramId") String telegramId) {
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
    ResponseEntity<ParticipantV1DTO> checkEduLogin(@RequestParam String login) throws ApiException {
        log.info("Получен запрос на проверку логина: login = {}", login);
        var participant = campusService.checkEduLogin(login);
        return ResponseEntity.ok(participant);
    }
}
