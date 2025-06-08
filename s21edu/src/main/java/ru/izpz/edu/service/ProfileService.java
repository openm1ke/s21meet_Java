package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.edu.exception.ProfileNotFoundException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true", matchIfMissing = true)
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final ProfileRepository profileRepository;
    private final ProfileValidationRepository profileValidationRepository;

    public ProfileDto getOrCreateProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
                .map(profileMapper::toDto)
                .orElseGet(() -> {
                    Profile profile = new Profile();
                    profile.setTelegramId(telegramId);
                    profile.setStatus(ProfileStatus.CREATED);
                    Profile saved = profileRepository.save(profile);
                    return profileMapper.toDto(saved);
                });
    }

    public ProfileDto updateProfileStatus(ProfileRequest request) {
        return profileRepository.findByTelegramId(request.getTelegramId())
                .map(existing -> {
                    existing.setStatus(request.getStatus());
                    return profileRepository.save(existing);
                })
                .map(profileMapper::toDto)
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + request.getTelegramId()));
    }

    public ProfileDto checkAndSetLogin(String telegramId, String s21login) {
        if (profileRepository.existsByS21login(s21login)) {
            throw new IllegalStateException("Логин " + s21login + " уже привязан к другому профилю");
        }
        Profile profile = profileRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + telegramId));
        if (profile.getS21login() != null) {
            throw new IllegalStateException("Профиль уже привязан к логину " + profile.getS21login());
        }
        profile.setS21login(s21login);
        return profileMapper.toDto(profileRepository.save(profile));
    }
}
