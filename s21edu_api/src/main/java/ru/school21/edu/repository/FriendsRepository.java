package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.school21.edu.model.Friends;

import java.util.List;
import java.util.UUID;

public interface FriendsRepository extends JpaRepository<Friends, UUID> {
    @Query("SELECT DISTINCT f.login FROM Friends f WHERE f.isSubscribe = true")
    List<String> findDistinctLogins();
}
