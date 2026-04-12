package ru.izpz.web.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.security.TelegramWebAppAuthFilter;
import ru.izpz.web.service.ProjectDirectoryFacade;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectDirectoryApiController {

    private final ProjectDirectoryFacade projectDirectoryFacade;

    @GetMapping("/names")
    public List<String> getProjectNames(
            HttpServletRequest request,
            @RequestParam(name = "all", defaultValue = "false") boolean all) {
        if (all) {
            return projectDirectoryFacade.getAllProjectNames();
        }
        Object attr = request.getAttribute(TelegramWebAppAuthFilter.TELEGRAM_ID_ATTR);
        String telegramId = attr == null ? null : attr.toString();
        if (telegramId == null || telegramId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telegram user is not resolved");
        }
        return projectDirectoryFacade.getProjectNames(telegramId);
    }

    @PostMapping("/executors")
    public List<ProjectExecutorDto> getProjectExecutors(@Valid @RequestBody ProjectExecutorsRequest request) {
        return projectDirectoryFacade.getProjectExecutors(request);
    }
}
