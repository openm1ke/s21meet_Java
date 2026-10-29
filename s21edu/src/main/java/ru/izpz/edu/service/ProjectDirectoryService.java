package ru.izpz.edu.service;

import java.util.Comparator;
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

  private static final String EMPTY_CAMPUS_FILTER = "__EMPTY__";

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
                  executor.login(),
                  campusName,
                  executor.projectStatus(),
                  executor.campusPlace(),
                  executor.wave());
            })
        .toList();
  }

  public ProjectExecutorsPageDto getProjectExecutorsPage(ProjectExecutorsPageRequest request) {
    String normalizedName = request.projectName().strip();
    List<String> campuses = normalizeCampuses(request.campuses());
    List<String> statuses = normalizeValues(request.statuses());
    Sort.Direction direction =
        "desc".equalsIgnoreCase(request.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
    PageRequest pageRequest =
        PageRequest.of(request.page(), request.size(), Sort.by(direction, "login"));
    String escapedProjectName = escapeLikePattern(normalizedName);
    List<ProjectExecutorDto> items;
    long totalItems;
    int totalPages;
    if (campuses.isEmpty() && statuses.isEmpty()) {
      List<String> campusQueryValues = List.of("__NO_CAMPUS__");
      List<String> statusQueryValues = List.of("__NO_STATUS__");
      Page<ProjectExecutorDto> page =
          studentProjectRepository.findExecutorsByProjectNamePaged(
              escapedProjectName, campusQueryValues, true, statusQueryValues, true, pageRequest);
      items = enrichCampuses(page.getContent());
      totalItems = page.getTotalElements();
      totalPages = page.getTotalPages();
    } else {
      List<ProjectExecutorDto> filteredExecutors =
          enrichCampuses(studentProjectRepository.findExecutorsByProjectName(escapedProjectName))
              .stream()
              .filter(executor -> matchesCampus(executor.campusName(), campuses))
              .filter(executor -> statuses.isEmpty() || statuses.contains(executor.projectStatus()))
              .sorted(executorComparator(direction))
              .toList();
      totalItems = filteredExecutors.size();
      totalPages = (int) Math.ceil((double) totalItems / request.size());
      int fromIndex = Math.min(request.page() * request.size(), filteredExecutors.size());
      int toIndex = Math.min(fromIndex + request.size(), filteredExecutors.size());
      items = filteredExecutors.subList(fromIndex, toIndex);
    }
    List<String> availableCampuses =
        studentProjectRepository.findDistinctCampusesByProjectName(escapedProjectName).stream()
            .map(this::normalizeCampusCode)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    List<String> availableStatuses =
        studentProjectRepository.findDistinctStatusesByProjectName(escapedProjectName).stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .distinct()
            .sorted()
            .toList();
    return new ProjectExecutorsPageDto(
        items,
        request.page(),
        request.size(),
        totalItems,
        totalPages,
        availableCampuses,
        availableStatuses);
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
                  executor.login(),
                  campusName,
                  executor.projectStatus(),
                  executor.campusPlace(),
                  executor.wave());
            })
        .toList();
  }

  private List<String> normalizeCampuses(List<String> values) {
    return normalizeValues(values).stream()
        .map(this::campusFilterKey)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private boolean matchesCampus(String campusName, List<String> campuses) {
    return campuses.isEmpty() || campuses.contains(campusFilterKey(campusName));
  }

  private String campusFilterKey(String campusName) {
    if (campusName == null || campusName.isBlank()) {
      return EMPTY_CAMPUS_FILTER;
    }
    String normalized = campusName.strip().toUpperCase(Locale.ROOT);
    if (EMPTY_CAMPUS_FILTER.equals(normalized)) {
      return normalized;
    }
    return normalized.replaceFirst("^\\d+\\s+", "");
  }

  private Comparator<ProjectExecutorDto> executorComparator(Sort.Direction direction) {
    Comparator<ProjectExecutorDto> comparator =
        Comparator.comparing(
            ProjectExecutorDto::login, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    return direction == Sort.Direction.DESC ? comparator.reversed() : comparator;
  }

  private List<String> normalizeValues(List<String> values) {
    return values == null
        ? List.of()
        : values.stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
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
