package ru.school21.edu.service;

import org.springframework.stereotype.Service;
import ru.school21.edu.repository.FriendsRepository;
import ru.school21.edu.repository.OnlineRepository;
import ru.school21.edu.repository.WorkplaceRepository;

import java.util.List;

@Service
public class NotifyService {

    private final FriendsRepository friendsRepository;
    private final WorkplaceRepository workplaceRepository;
    private final OnlineRepository onlineRepository;

    public NotifyService(FriendsRepository friendsRepository, WorkplaceRepository workplaceRepository, OnlineRepository onlineRepository) {
        this.friendsRepository = friendsRepository;
        this.workplaceRepository = workplaceRepository;
        this.onlineRepository = onlineRepository;
    }

    public void startNotificate() {
        List<String> subsribeLogins = friendsRepository.findDistinctLogins();

    }
}
