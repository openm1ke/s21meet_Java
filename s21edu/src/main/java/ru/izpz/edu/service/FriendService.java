package ru.izpz.edu.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendRequest;
import ru.izpz.dto.FriendsSliceDto;
import ru.izpz.edu.mapper.FriendsMapper;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.repository.FriendsRepository;

import java.time.LocalDateTime;
import java.util.List;

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
            case TOGGLE_FRIEND -> {
                f.setIsFriend(!Boolean.TRUE.equals(f.getIsFriend()));
                if (Boolean.FALSE.equals(f.getIsFriend())) {
                    f.setIsFavorite(false);
                    f.setIsSubscribe(false);
                    f.setName("");
                }
            }
            case TOGGLE_FAVORITE -> f.setIsFavorite(!Boolean.TRUE.equals(f.getIsFavorite()));
            case TOGGLE_SUBSCRIBE -> f.setIsSubscribe(!Boolean.TRUE.equals(f.getIsSubscribe()));
            case SET_NAME -> f.setName(name == null ? "" : name.trim());
            case NONE -> {
                return friendsMapper.toDto(f);
            }
        }

        return friendsMapper.toDto(friendsRepository.save(f));
    }

    public FriendsSliceDto getFriends(String telegramId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Friends> slice = friendsRepository.findAllOrdered(telegramId, pageable);
        List<FriendDto> items = friendsMapper.toDtos(slice.getContent());
        return new FriendsSliceDto(items, page, size, slice.hasNext());
    }
}
