package ru.izpz.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectDirectoryFacade {

    private final EduProfileClient eduProfileClient;

    public List<String> getProjectNames(String telegramId) {
        return eduProfileClient.getProjectNames(new CampusRequest(telegramId));
    }

    public List<String> getAllProjectNames() {
        return eduProfileClient.getAllProjectNames();
    }

    public List<ProjectExecutorDto> getProjectExecutors(ProjectExecutorsRequest request) {
        return eduProfileClient.getProjectExecutors(request);
    }
}
