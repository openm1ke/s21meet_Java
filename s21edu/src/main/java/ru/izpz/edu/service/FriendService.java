package ru.izpz.edu.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendRequest;
import ru.izpz.dto.FriendsSliceDto;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.edu.mapper.FriendsMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.ParticipantRepository;
import ru.izpz.edu.repository.ParticipantView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendsRepository friendsRepository;
    private final FriendsMapper friendsMapper;
    private final WorkplaceService workplaceService;
    private final ClusterRepository clusterRepository;
    private final ParticipantRepository participantRepository;

    public FriendDto applyFriend(String telegramId, @NotBlank String login, FriendRequest.@NotNull Action action, String name) {
        Friends f = friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login)
            .orElseGet(() -> {
                var nf = new Friends();
                nf.setTelegramId(telegramId);
                nf.setLogin(login);
                nf.setDate(LocalDateTime.now());
                return friendsRepository.save(nf);
            });
        log.info("applyFriend: {}", f);
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

        List<String> friendLogins = slice.getContent().stream()
                .map(Friends::getLogin)
                .toList();

        Map<String, Workplace> occupied = workplaceService.findAllByLoginIn(friendLogins).stream()
                .collect(Collectors.toMap(Workplace::getLogin, w -> w, (a, b) -> a));

        Set<Long> clusterIds = occupied.values().stream()
                .map(w -> w.getId().getClusterId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> clusterNameById = clusterRepository.findAllByClusterIdIn(clusterIds).stream()
                .collect(Collectors.toMap(Cluster::getClusterId, Cluster::getName));

        Map<String, ParticipantStatusEnum> statusByLogin = participantRepository.findAllViewByLoginIn(friendLogins).stream()
            .collect(Collectors.toMap(
                ParticipantView::getLogin,
                ParticipantView::getStatus,
                (a, b) -> a
            ));

        List<FriendDto> items = slice.getContent().stream()
                .map(f -> {
                    FriendDto base = friendsMapper.toDto(f);
                    Workplace seat = occupied.get(f.getLogin());
                    base.setIsOnline(seat != null);
                    if (seat != null) {
                        base.setClusterName(clusterNameById.get(seat.getId().getClusterId()));
                        base.setStageGroupName(seat.getStageGroupName());
                        base.setStageName(seat.getStageName());
                        base.setRow(seat.getId().getRow());
                        base.setNumber(seat.getId().getNumber());
                    }
                    base.setStatus(statusByLogin.get(f.getLogin()));
                    return base;
                })
                .toList();

        return new FriendsSliceDto(items, page, size, slice.hasNext());

//        List<FriendDto> items = friendsMapper.toDtos(slice.getContent());
//        return new FriendsSliceDto(items, page, size, slice.hasNext());
    }
}
