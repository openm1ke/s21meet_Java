package ru.izpz.edu.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ParticipantProjectV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;
import ru.izpz.edu.service.StudentProjectRefreshService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"profile.service.enabled", "api.participant.enabled"}, havingValue = "true")
public class RestApiProjectsProvider implements ProjectsProvider {

    private static final String STATUS_ASSIGNED = "ASSIGNED";

    private final RestProjectsApiFacade restProjectsApiFacade;
    private final ProjectsProviderConfig.ProjectsProperties projectsProperties;
    private final StudentProjectRepository studentProjectRepository;
    private final StudentProjectRefreshService studentProjectRefreshService;
    private final StudentCredentialsRepository studentCredentialsRepository;

    @Override
    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        try {
            refreshStudentProjectsByLogin(login);
        } catch (RuntimeException e) {
            log.warn("Не удалось обновить кэш проектов для {}: {}", login, e.getMessage());
        }
        return getCachedStudentProjectsByLogin(login);
    }

    @Override
    public void refreshStudentProjectsByLogin(String login) {
        if (!isProjectsRefreshRequired(login)) {
            return;
        }

        FetchResult fetched = fetchAllStatuses(login);
        if (!fetched.success()) {
            throw new ProjectsRefreshException("Не удалось получить проекты участника");
        }

        List<StudentProjectData> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        appendDistinct(result, seenIds, fetched.projects());

        String userId = studentCredentialsRepository.findById(login)
            .map(StudentCredentials::getUserId)
            .orElse(null);
        studentProjectRefreshService.replaceProjects(login, userId, result);
    }

    private List<StudentProjectData> getCachedStudentProjectsByLogin(String login) {
        return studentProjectRepository.findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(login).stream()
            .map(this::toDto)
            .toList();
    }

    private boolean isProjectsRefreshRequired(String login) {
        return java.util.Optional.ofNullable(studentProjectRepository.findMaxUpdatedAtByLogin(login))
            .map(updatedAt -> updatedAt.isBefore(OffsetDateTime.now().minus(projectsProperties.getRefreshTtl())))
            .orElse(true);
    }

    private FetchResult fetchAllStatuses(String login) {
        long pageSize = Math.max(1, projectsProperties.getRest().getPageSize());
        try {
            ParticipantProjectsV1DTO response = restProjectsApiFacade.getParticipantProjectsByLogin(login, pageSize, null);
            if (response == null || response.getProjects() == null) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(response.getProjects());
        } catch (ApiException e) {
            log.warn("Не удалось получить проекты для {}: {}", login, e.getMessage());
            return FetchResult.failure();
        } catch (RuntimeException e) {
            log.warn("Непредвиденная ошибка получения проектов для {}: {}", login, e.getMessage());
            return FetchResult.failure();
        }
    }

    private void appendDistinct(
            List<StudentProjectData> target,
            Set<Long> seenIds,
            List<ParticipantProjectV1DTO> projects
    ) {
        for (ParticipantProjectV1DTO project : projects) {
            if (project != null && project.getId() != null && !isAssigned(project)) {
                long projectId = project.getId();
                if (seenIds.add(projectId)) {
                    target.add(toProjectData(project));
                }
            }
        }
    }

    private boolean isAssigned(ParticipantProjectV1DTO project) {
        String status = toNullableString(project.getStatus());
        return STATUS_ASSIGNED.equalsIgnoreCase(status);
    }

    private StudentProjectData toProjectData(ParticipantProjectV1DTO project) {
        return new StudentProjectData(
                Long.toString(project.getId()),
                project.getTitle(),
                null,
                null,
                toDateTimeString(project.getCompletionDateTime()),
                project.getFinalPercentage(),
                null,
                toNullableString(project.getType()),
                toNullableString(project.getStatus()),
                project.getTeamMembers() == null ? null : project.getTeamMembers().size(),
                toInteger(project.getCourseId())
        );
    }

    private String toDateTimeString(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

    private String toNullableString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer toInteger(Long value) {
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }

    private StudentProjectData toDto(StudentProject project) {
        return new StudentProjectData(
            project.getGoalId(),
            project.getName(),
            project.getDescription(),
            project.getExperience(),
            project.getDateTime(),
            project.getFinalPercentage(),
            project.getLaboriousness(),
            project.getExecutionType(),
            project.getGoalStatus(),
            project.getAmountMembers(),
            project.getLocalCourseId()
        );
    }

    private record FetchResult(boolean success, List<ParticipantProjectV1DTO> projects) {
        private static FetchResult success(List<ParticipantProjectV1DTO> projects) {
            return new FetchResult(true, projects);
        }

        private static FetchResult failure() {
            return new FetchResult(false, List.of());
        }
    }

    private static class ProjectsRefreshException extends RuntimeException {
        private ProjectsRefreshException(String message) {
            super(message);
        }
    }
}
