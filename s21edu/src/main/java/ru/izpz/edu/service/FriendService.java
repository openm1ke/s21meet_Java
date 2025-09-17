package ru.izpz.edu.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendRequest;
import ru.izpz.edu.mapper.FriendsMapper;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.repository.FriendsRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendsRepository friendsRepository;
    private final FriendsMapper friendsMapper;

    public FriendDto applyFriend(String telegramId, @NotBlank String login, FriendRequest.@NotNull Action action, String name) {
        Friends f = friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login)
            .orElseGet(() -> {
                var nf = new Friends();
                nf.setTelegramId(telegramId);
                nf.setLogin(login);
                nf.setDate(LocalDateTime.now());
                return friendsRepository.save(nf);
            });

        switch (action) {
            case TOGGLE_FRIEND     -> f.setIsFriend(!Boolean.TRUE.equals(f.getIsFriend()));
            case TOGGLE_FAVORITE   -> f.setIsFavorite(!Boolean.TRUE.equals(f.getIsFavorite()));
            case TOGGLE_SUBSCRIBE  -> f.setIsSubscribe(!Boolean.TRUE.equals(f.getIsSubscribe()));
            case SET_NAME          -> f.setName(name == null ? "" : name.trim());
        }

        return friendsMapper.toDto(friendsRepository.save(f));
    }
}
