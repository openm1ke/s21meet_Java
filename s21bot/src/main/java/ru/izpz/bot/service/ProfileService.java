package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.bot.dto.ProfileDto;
import ru.izpz.bot.dto.ProfileRequest;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileClient profileClient;

    public ProfileDto getProfile(ProfileRequest request) {
        return profileClient.getOrCreateProfile(request);
    }
}
