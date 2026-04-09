package ru.izpz.edu.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.repository.StudentCredentialsRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class CampusRoutingProjectsProvider {

    private static final String DEFAULT_MSK_SCHOOL_ID = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";

    private final ObjectProvider<GraphQLProjectsProvider> graphQlProvider;
    private final ObjectProvider<RestApiProjectsProvider> restProvider;
    private final StudentCredentialsRepository studentCredentialsRepository;

    @Value("${projects.routing.graphql-school-id:${projects.scheduler.graphql-school-id:" + DEFAULT_MSK_SCHOOL_ID + "}}")
    private String graphQlSchoolId;

    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        ProjectsProvider provider = resolveProviderForLogin(login);
        if (provider == null) {
            log.warn("Projects provider недоступен для login={}", login);
            return List.of();
        }
        return provider.getStudentProjectsByLogin(login);
    }

    public RefreshResult refreshStudentProjects(StudentCredentials credentials) {
        return refreshStudentProjects(credentials.getLogin(), credentials.getSchoolId());
    }

    public RefreshResult refreshStudentProjects(String login) {
        String schoolId = studentCredentialsRepository.findById(login)
            .map(StudentCredentials::getSchoolId)
            .orElse(null);
        return refreshStudentProjects(login, schoolId);
    }

    public String providerTypeForSchoolId(String schoolId) {
        return providerType(resolveProvider(schoolId));
    }

    private RefreshResult refreshStudentProjects(String login, String schoolId) {
        ProjectsProvider provider = resolveProvider(schoolId);
        if (provider == null) {
            return RefreshResult.SKIPPED_NO_PROVIDER;
        }
        provider.refreshStudentProjectsByLogin(login);
        return RefreshResult.SUCCESS;
    }

    private ProjectsProvider resolveProviderForLogin(String login) {
        String schoolId = studentCredentialsRepository.findById(login)
            .map(StudentCredentials::getSchoolId)
            .orElse(null);
        return resolveProvider(schoolId);
    }

    private ProjectsProvider resolveProvider(String schoolId) {
        ProjectsProvider graphQl = graphQlProvider.getIfAvailable();
        ProjectsProvider rest = restProvider.getIfAvailable();

        boolean useGraphQl = graphQlSchoolId != null && graphQlSchoolId.equals(schoolId);
        if (useGraphQl) {
            if (graphQl != null) {
                return graphQl;
            }
            return rest;
        }

        if (rest != null) {
            return rest;
        }
        return graphQl;
    }

    private String providerType(ProjectsProvider provider) {
        if (provider == null) {
            return "none";
        }
        if (provider instanceof GraphQLProjectsProvider) {
            return "graphql";
        }
        if (provider instanceof RestApiProjectsProvider) {
            return "rest";
        }
        return provider.getClass().getSimpleName();
    }

    public enum RefreshResult {
        SUCCESS,
        SKIPPED_NO_PROVIDER
    }
}
