package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectDirectoryService {

    private final StudentProjectRepository studentProjectRepository;
    private final StudentCredentialsRepository studentCredentialsRepository;
    private final CampusCatalog campusCatalog;

    public List<String> getProjectNames() {
        return studentProjectRepository.findDistinctActualProjectNames();
    }

    public List<ProjectExecutorDto> getProjectExecutors(String projectName) {
        String normalizedName = projectName == null ? "" : projectName.strip();
        if (normalizedName.isEmpty()) {
            return List.of();
        }
        List<ProjectExecutorDto> executors = studentProjectRepository.findExecutorsByProjectName(escapeLikePattern(normalizedName));
        if (executors.isEmpty()) {
            return executors;
        }

        List<String> logins = executors.stream()
            .map(ProjectExecutorDto::login)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<String, String> campusByLogin = studentCredentialsRepository.findSchoolIdsByLogins(logins).stream()
            .filter(row -> row.getSchoolId() != null)
            .collect(Collectors.toMap(
                StudentCredentialsRepository.LoginSchoolIdView::getLogin,
                row -> campusCatalog.campusName(row.getSchoolId()),
                (left, right) -> left
            ));

        return executors.stream()
            .map(executor -> {
                String campusName = executor.campusName();
                if ((campusName == null || campusName.isBlank()) && executor.login() != null) {
                    campusName = campusByLogin.get(executor.login());
                }
                return new ProjectExecutorDto(
                    executor.login(),
                    campusName,
                    executor.projectStatus(),
                    executor.campusPlace()
                );
            })
            .toList();
    }

    private String escapeLikePattern(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '%' || ch == '_') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }
}
