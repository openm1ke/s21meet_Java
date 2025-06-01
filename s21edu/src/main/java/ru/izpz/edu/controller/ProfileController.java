package ru.izpz.edu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.edu.service.ProfileService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@RequestParam("telegramId") String telegramId) {
        log.info("Получен запрос на вывод профиля для telegramId = {}", telegramId);
        var profile = profileService.getOrCreateProfile(telegramId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<ProfileDto> updateProfile(@Valid @RequestBody ProfileRequest request) {
        log.info("Получен запрос на обновление профиля для telegramId = {}", request.getTelegramId());
        var updateProfile = profileService.updateProfile(request);
        return ResponseEntity.ok(updateProfile);
    }
}
