package ru.izpz.web.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsPageDto;
import ru.izpz.dto.ProjectExecutorsPageRequest;
import ru.izpz.dto.ProjectExecutorsRequest;

@FeignClient(name = "profile", url = "${profile.service.url}", path = "/profile")
public interface EduProfileClient {

  @PostMapping("/project-names")
  List<String> getProjectNames(@RequestBody CampusRequest request);

  @PostMapping("/project-names/all")
  List<String> getAllProjectNames();

  @PostMapping("/project-executors")
  List<ProjectExecutorDto> getProjectExecutors(@RequestBody ProjectExecutorsRequest request);

  @PostMapping("/project-executors/page")
  ProjectExecutorsPageDto getProjectExecutorsPage(
      @RequestBody ProjectExecutorsPageRequest request);
}
