package ru.izpz.edu.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsPageDto;
import ru.izpz.dto.ProjectExecutorsPageRequest;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

@Service
@RequiredArgsConstructor
public class ProjectDirectoryService {

  private final StudentProjectRepository studentProjectRepository;
  private final StudentCredentialsRepository studentCredentialsRepository;
  private final CampusCatalog campusCatalog;

  public List<String> getProjectNames() {
    return studentProjectRepository.findDistinctActualProjectNames();
  }

  public List<ProjectExecutorDto> getProjectExecutors(ProjectExecutorsRequest request) {
    String normalizedName =
        request == null || request.projectName() == null ? "" : request.projectName().strip();
    if (normalizedName.isEmpty()) {
      return List.of();
    }
    List<ProjectExecutorDto> executors =
        studentProjectRepository.findExecutorsByProjectName(escapeLikePattern(normalizedName));
    if (executors.isEmpty()) {
      return executors;
    }

    List<String> logins =
        executors.stream()
            .map(ProjectExecutorDto::login)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<String, String> campusByLogin =
        studentCredentialsRepository.findSchoolIdsByLogins(logins).stream()
            .filter(row -> row.getSchoolId() != null)
            .collect(
                Collectors.toMap(
                    StudentCredentialsRepository.LoginSchoolIdView::getLogin,
                    row -> campusCatalog.campusName(row.getSchoolId()),
                    (left, right) -> left));

    return executors.stream()
        .map(
            executor -> {
              String campusName = normalizeCampusCode(executor.campusName());
              if ((campusName == null || campusName.isBlank()) && executor.login() != null) {
                campusName = normalizeCampusCode(campusByLogin.get(executor.login()));
              }
              return new ProjectExecutorDto(
                  executor.login(), campusName, executor.projectStatus(), executor.campusPlace(), executor.wave());
            })
        .toList();
  }

  public ProjectExecutorsPageDto getProjectExecutorsPage(ProjectExecutorsPageRequest request) {
    String normalizedName = request.projectName().strip();
    List<String> campuses = normalizeCampuses(request.campuses());
    List<String> statuses = normalizeValues(request.statuses());
    Sort.Direction direction = "desc".equalsIgnoreCase(request.sortDirection())
        ? Sort.Direction.DESC : Sort.Direction.ASC;
    List<String> campusQueryValues = campuses.isEmpty() ? List.of("__NO_CAMPUS__") : campuses;
    List<String> statusQueryValues = statuses.isEmpty() ? List.of("__NO_STATUS__") : statuses;
    PageRequest pageRequest =
        PageRequest.of(request.page(), request.size(), Sort.by(direction, "login"));
    Page<ProjectExecutorDto> page =
        studentProjectRepository.findExecutorsByProjectNamePaged(
            escapeLikePattern(normalizedName),
            campusQueryValues,
            campuses.isEmpty(),
            statusQueryValues,
            statuses.isEmpty(),
            pageRequest);
    List<ProjectExecutorDto> items = enrichCampuses(page.getContent());
    List<String> availableCampuses = studentProjectRepository.findDistinctCampusesByProjectName(
        escapeLikePattern(normalizedName)).stream().map(this::normalizeCampusCode).filter(Objects::nonNull)
        .distinct().sorted().toList();
    List<String> availableStatuses = studentProjectRepository.findDistinctStatusesByProjectName(
        escapeLikePattern(normalizedName)).stream().filter(Objects::nonNull).map(String::strip)
        .filter(s -> !s.isEmpty()).distinct().sorted().toList();
    return new ProjectExecutorsPageDto(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
        availableCampuses, availableStatuses);
  }

  private List<ProjectExecutorDto> enrichCampuses(List<ProjectExecutorDto> executors) {
    if (executors.isEmpty()) {
      return executors;
    }
    List<String> logins =
        executors.stream()
            .map(ProjectExecutorDto::login)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<String, String> campusByLogin =
        studentCredentialsRepository.findSchoolIdsByLogins(logins).stream()
            .filter(row -> row.getSchoolId() != null)
            .collect(
                Collectors.toMap(
                    StudentCredentialsRepository.LoginSchoolIdView::getLogin,
                    row -> campusCatalog.campusName(row.getSchoolId()),
                    (left, right) -> left));
    return executors.stream()
        .map(
            executor -> {
              String campusName = normalizeCampusCode(executor.campusName());
              if ((campusName == null || campusName.isBlank()) && executor.login() != null) {
                campusName = normalizeCampusCode(campusByLogin.get(executor.login()));
              }
              return new ProjectExecutorDto(
                  executor.login(), campusName, executor.projectStatus(), executor.campusPlace(), executor.wave());
            })
        .toList();
  }

  private List<String> normalizeCampuses(List<String> values) {
    return normalizeValues(values).stream().map(value -> value.toUpperCase(Locale.ROOT)).toList();
  }

  private List<String> normalizeValues(List<String> values) {
    return values == null
        ? List.of()
        : values.stream().filter(Objects::nonNull).map(String::strip).filter(s -> !s.isEmpty())
            .distinct().toList();
  }

  private String normalizeCampusCode(String campusName) {
    if (campusName == null || campusName.isBlank()) {
      return null;
    }
    String normalized = campusName.trim().toUpperCase(Locale.ROOT);
    if ("MSK".equals(normalized) || normalized.contains("MOSCOW")) {
      return "MSK";
    }
    if ("KZN".equals(normalized) || normalized.contains("KAZAN")) {
      return "KZN";
    }
    if ("NSK".equals(normalized) || normalized.contains("NOVOSIBIRSK")) {
      return "NSK";
    }
    return normalized;
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
