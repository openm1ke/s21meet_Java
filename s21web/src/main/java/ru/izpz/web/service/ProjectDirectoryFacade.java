package ru.izpz.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectDirectoryFacade {

    private final EduProfileClient eduProfileClient;

    public List<String> getProjectNames() {
        return eduProfileClient.getProjectNames();
    }

    public List<ProjectExecutorDto> getProjectExecutors(String projectName) {
        return eduProfileClient.getProjectExecutors(new ProjectExecutorsRequest(projectName));
    }
}
