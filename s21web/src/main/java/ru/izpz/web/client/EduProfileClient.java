package ru.izpz.web.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;

import java.util.List;

@FeignClient(
        name = "profile",
        url = "${profile.service.url}",
        path = "/profile"
)
public interface EduProfileClient {

    @PostMapping("/project-names")
    List<String> getProjectNames();

    @PostMapping("/project-executors")
    List<ProjectExecutorDto> getProjectExecutors(@RequestBody ProjectExecutorsRequest request);
}
