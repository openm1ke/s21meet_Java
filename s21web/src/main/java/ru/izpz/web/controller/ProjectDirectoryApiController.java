package ru.izpz.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.service.ProjectDirectoryFacade;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectDirectoryApiController {

    private final ProjectDirectoryFacade projectDirectoryFacade;

    @GetMapping("/names")
    public List<String> getProjectNames() {
        return projectDirectoryFacade.getProjectNames();
    }

    @PostMapping("/executors")
    public List<ProjectExecutorDto> getProjectExecutors(@Valid @RequestBody ProjectExecutorsRequest request) {
        return projectDirectoryFacade.getProjectExecutors(request.projectName());
    }
}
