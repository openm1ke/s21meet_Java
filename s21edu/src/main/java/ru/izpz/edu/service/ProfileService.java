package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.edu.dto.ProfileDto;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileStatus;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileValidationRepository profileValidationRepository;

    public ProfileDto viewProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
            .map(ProfileDto::fromEntity)
            .orElseGet(() -> {
                Profile profile = new Profile();
                profile.setTelegramId(telegramId);
                profile.setStatus(ProfileStatus.CREATED);
                Profile saved = profileRepository.save(profile);
                return ProfileDto.fromEntity(saved);
            });
    }
}
