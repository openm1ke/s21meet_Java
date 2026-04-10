package ru.izpz.edu.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentCredentials;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class CampusRoutingProjectsProvider {

    private final ObjectProvider<RestApiProjectsProvider> restProvider;

    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        ProjectsProvider provider = resolveProviderForLogin();
        if (provider == null) {
            log.warn("Projects provider недоступен для login={}", login);
            return List.of();
        }
        return provider.getStudentProjectsByLogin(login);
    }

    public RefreshResult refreshStudentProjects(StudentCredentials credentials) {
        return refreshStudentProjects(credentials.getLogin());
    }

    public RefreshResult refreshStudentProjects(String login) {
        ProjectsProvider provider = resolveProvider();
        if (provider == null) {
            return RefreshResult.SKIPPED_NO_PROVIDER;
        }
        provider.refreshStudentProjectsByLogin(login);
        return RefreshResult.SUCCESS;
    }

    public String providerTypeForSchoolId() {
        return providerType(resolveProvider());
    }

    private ProjectsProvider resolveProviderForLogin() {
        return resolveProvider();
    }

    private ProjectsProvider resolveProvider() {
        return restProvider.getIfAvailable();
    }

    private String providerType(ProjectsProvider provider) {
        if (provider == null) {
            return "none";
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
