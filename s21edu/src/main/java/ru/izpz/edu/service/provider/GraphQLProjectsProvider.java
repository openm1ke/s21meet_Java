package ru.izpz.edu.service.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"profile.service.enabled", "api.graphql.enabled"}, havingValue = "true")
public class GraphQLProjectsProvider implements ProjectsProvider {

    private final GraphQLService graphQLService;

    @Override
    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        return graphQLService.getCachedStudentProjectsByLogin(login);
    }

    @Override
    public void refreshStudentProjectsByLogin(String login) {
        graphQLService.refreshStudentProjectsByLoginWithLimits(login);
    }
}
