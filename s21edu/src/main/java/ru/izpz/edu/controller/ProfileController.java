package ru.izpz.edu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.izpz.edu.dto.ProfileDto;
import ru.izpz.edu.dto.ProfileRequest;
import ru.izpz.edu.service.ProfileService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<ProfileDto> profile(@Valid @RequestBody ProfileRequest request) {
        String telegramId = request.getTelegramId();
        log.info("Получен запрос на получение профиля для telegramId = {}", telegramId);
        var profile = profileService.viewProfile(telegramId);
        return ResponseEntity.ok(profile);
    }
}
