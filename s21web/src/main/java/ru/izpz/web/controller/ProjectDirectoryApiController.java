package ru.izpz.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsPageDto;
import ru.izpz.dto.ProjectExecutorsPageRequest;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.security.TelegramWebAppAuthFilter;
import ru.izpz.web.service.ProjectDirectoryFacade;

@Validated
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/projects")
public class ProjectDirectoryApiController {

  private final ProjectDirectoryFacade projectDirectoryFacade;

  @GetMapping("/names")
  public ResponseEntity<List<String>> getProjectNames(
      HttpServletRequest request, @RequestParam(name = "all", defaultValue = "false") boolean all) {
    Object attr = request.getAttribute(TelegramWebAppAuthFilter.TELEGRAM_ID_ATTR);
    String telegramId = attr == null ? null : attr.toString();
    log.info(
        "Web App project names request: all={}, telegramIdPresent={}, telegramIdSuffix={}",
        all,
        telegramId != null && !telegramId.isBlank(),
        telegramIdSuffix(telegramId));

    if (all) {
      List<String> projectNames = projectDirectoryFacade.getAllProjectNames();
      log.info("Web App project names response: all=true, count={}", projectNames.size());
      return ResponseEntity.ok()
          .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
          .body(projectNames);
    }
    if (telegramId == null || telegramId.isBlank()) {
      log.warn("Web App project names rejected: Telegram ID was not resolved");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telegram user is not resolved");
    }
    List<String> projectNames = projectDirectoryFacade.getProjectNames(telegramId);
    log.info(
        "Web App project names response: all=false, telegramIdSuffix={}, count={}",
        telegramIdSuffix(telegramId),
        projectNames.size());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePrivate())
        .body(projectNames);
  }

  private static String telegramIdSuffix(String telegramId) {
    if (telegramId == null || telegramId.isBlank()) {
      return "-";
    }
    return telegramId.length() <= 4
        ? telegramId
        : "..." + telegramId.substring(telegramId.length() - 4);
  }

  @PostMapping("/executors")
  public List<ProjectExecutorDto> getProjectExecutors(
      @Valid @RequestBody ProjectExecutorsRequest request) {
    return projectDirectoryFacade.getProjectExecutors(request);
  }

  @PostMapping("/executors/page")
  public ProjectExecutorsPageDto getProjectExecutorsPage(
      @Valid @RequestBody ProjectExecutorsPageRequest request) {
    return projectDirectoryFacade.getProjectExecutorsPage(request);
  }
}
