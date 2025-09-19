package ru.izpz.edu.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.Friends;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendsRepository extends JpaRepository<Friends, UUID> {
    @Query("SELECT DISTINCT f.login FROM Friends f WHERE f.isSubscribe = true")
    List<String> findDistinctLogins();

    List<Friends> findByLoginAndIsSubscribeTrue(String login);

    Optional<Friends> findFirstByTelegramIdAndLogin(String telegramId, String login);

    @Query("""
       select f from Friends f
       where f.telegramId = :telegramId and f.isFriend = true
       order by f.isFavorite desc, f.date desc
    """)
    Slice<Friends> findAllOrdered(@Param("telegramId") String telegramId, Pageable pageable);
}
