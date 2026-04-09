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

    private static final String STATUS_REGISTERED = "REGISTERED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

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

        FetchStatusResult inProgress = fetchByStatus(login, STATUS_IN_PROGRESS);
        FetchStatusResult registered = fetchByStatus(login, STATUS_REGISTERED);
        if (!inProgress.success() || !registered.success()) {
            throw new ProjectsRefreshException("Не удалось получить проекты по одному или нескольким статусам");
        }

        List<StudentProjectData> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        appendDistinct(result, seenIds, inProgress.projects(), STATUS_IN_PROGRESS);
        appendDistinct(result, seenIds, registered.projects(), STATUS_REGISTERED);

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

    private FetchStatusResult fetchByStatus(String login, String status) {
        long pageSize = Math.max(1, projectsProperties.getRest().getPageSize());
        try {
            ParticipantProjectsV1DTO response = restProjectsApiFacade.getParticipantProjectsByLogin(login, pageSize, status);
            if (response == null || response.getProjects() == null) {
                return FetchStatusResult.success(List.of());
            }
            return FetchStatusResult.success(response.getProjects());
        } catch (ApiException e) {
            log.warn("Не удалось получить проекты для {} со статусом {}: {}", login, status, e.getMessage());
            return FetchStatusResult.failure();
        } catch (RuntimeException e) {
            log.warn("Непредвиденная ошибка получения проектов для {} со статусом {}: {}", login, status, e.getMessage());
            return FetchStatusResult.failure();
        }
    }

    private void appendDistinct(
            List<StudentProjectData> target,
            Set<Long> seenIds,
            List<ParticipantProjectV1DTO> projects,
            String requestedStatus
    ) {
        for (ParticipantProjectV1DTO project : projects) {
            if (project != null && project.getId() != null) {
                long projectId = project.getId();
                if (seenIds.add(projectId)) {
                    target.add(toProjectData(project, requestedStatus));
                }
            }
        }
    }

    private StudentProjectData toProjectData(
            ParticipantProjectV1DTO project,
            String requestedStatus
    ) {
        return new StudentProjectData(
                Long.toString(project.getId()),
                project.getTitle(),
                null,
                null,
                toDateTimeString(project.getCompletionDateTime()),
                project.getFinalPercentage(),
                null,
                toNullableString(project.getType()),
                firstNonBlank(toNullableString(project.getStatus()), requestedStatus),
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
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

    private record FetchStatusResult(boolean success, List<ParticipantProjectV1DTO> projects) {
        private static FetchStatusResult success(List<ParticipantProjectV1DTO> projects) {
            return new FetchStatusResult(true, projects);
        }

        private static FetchStatusResult failure() {
            return new FetchStatusResult(false, List.of());
        }
    }

    private static class ProjectsRefreshException extends RuntimeException {
        private ProjectsRefreshException(String message) {
            super(message);
        }
    }
}
