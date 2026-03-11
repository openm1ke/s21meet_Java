package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private final WorkplaceRepository workplaceRepository;

    @Transactional(readOnly = true)
    public Optional<Workplace> getWorkplace(@RequestParam String login) {
        return workplaceRepository.findByLogin(login);
    }

    @Transactional(readOnly = true)
    public List<Workplace> getWorkplaces() {
        return workplaceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Workplace> findAllByLoginIn(List<String> friendLogins) {
        return workplaceRepository.findAllByLoginIn(friendLogins);
    }
}
