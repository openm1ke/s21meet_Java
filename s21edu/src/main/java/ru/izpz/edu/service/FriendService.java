package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.FriendDto;
import ru.izpz.edu.mapper.FriendsMapper;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.repository.FriendsRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendsRepository friendsRepository;
    private final FriendsMapper friendsMapper;

    public FriendDto getOrCreateFriend(String telegramId, String login) {
        return friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login)
            .map(existing -> {
                // переключаем флаг
                existing.setIsFriend(!Boolean.TRUE.equals(existing.getIsFriend()));
                Friends saved = friendsRepository.save(existing);
                return friendsMapper.toDto(saved);
            })
            .orElseGet(() -> {
                // создаём новую запись с isFriend = true
                Friends friends = new Friends();
                friends.setLogin(login);
                friends.setTelegramId(telegramId);
                friends.setIsFriend(true);
                friends.setDate(LocalDateTime.now());
                Friends saved = friendsRepository.save(friends);
                return friendsMapper.toDto(saved);
            });
    }
}
