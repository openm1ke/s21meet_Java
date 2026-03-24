package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.api.ProjectApi;
import ru.izpz.dto.model.ParticipantProjectV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.dto.model.ProjectV1DTO;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.dto.StudentProjectData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"profile.service.enabled", "api.participant.enabled", "api.project.enabled"}, havingValue = "true")
public class RestApiProjectsProvider implements ProjectsProvider {

    private static final String STATUS_REGISTERED = "REGISTERED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final ParticipantApi participantApi;
    private final ProjectApi projectApi;
    private final ProjectsProviderConfig.ProjectsProperties projectsProperties;

    @Override
    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        List<ParticipantProjectV1DTO> inProgress = fetchByStatus(login, STATUS_IN_PROGRESS);
        List<ParticipantProjectV1DTO> registered = fetchByStatus(login, STATUS_REGISTERED);

        List<StudentProjectData> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        Map<Long, ProjectV1DTO> detailsCache = new HashMap<>();
        appendDistinct(result, seenIds, detailsCache, inProgress, STATUS_IN_PROGRESS);
        appendDistinct(result, seenIds, detailsCache, registered, STATUS_REGISTERED);
        return result;
    }

    private List<ParticipantProjectV1DTO> fetchByStatus(String login, String status) {
        long pageSize = Math.max(1, projectsProperties.getRest().getPageSize());
        try {
            ParticipantProjectsV1DTO response = participantApi.getParticipantProjectsByLogin(login, pageSize, 0L, status);
            if (response == null || response.getProjects() == null) {
                return List.of();
            }
            return response.getProjects();
        } catch (ApiException e) {
            log.warn("Не удалось получить проекты для {} со статусом {}: {}", login, status, e.getMessage());
            return List.of();
        }
    }

    private void appendDistinct(
            List<StudentProjectData> target,
            Set<Long> seenIds,
            Map<Long, ProjectV1DTO> detailsCache,
            List<ParticipantProjectV1DTO> projects,
            String requestedStatus
    ) {
        for (ParticipantProjectV1DTO project : projects) {
            long projectId = project.getId();
            if (!seenIds.add(projectId)) {
                continue;
            }
            ProjectV1DTO details = resolveProjectDetails(projectId, detailsCache);
            target.add(toProjectData(project, details, requestedStatus));
        }
    }

    private ProjectV1DTO resolveProjectDetails(long projectId, Map<Long, ProjectV1DTO> detailsCache) {
        if (detailsCache.containsKey(projectId)) {
            return detailsCache.get(projectId);
        }
        try {
            ProjectV1DTO details = projectApi.getProjectByProjectId(projectId);
            detailsCache.put(projectId, details);
            return details;
        } catch (ApiException e) {
            log.warn("Не удалось получить детальную информацию проекта {}: {}", projectId, e.getMessage());
            detailsCache.put(projectId, null);
            return null;
        }
    }

    private StudentProjectData toProjectData(
            ParticipantProjectV1DTO project,
            ProjectV1DTO details,
            String requestedStatus
    ) {
        return new StudentProjectData(
                Long.toString(project.getId()),
                project.getTitle(),
                details == null ? null : details.getDescription(),
                details == null ? null : details.getXp(),
                toDateTimeString(project.getCompletionDateTime()),
                project.getFinalPercentage(),
                details == null ? null : details.getDurationHours(),
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
}
