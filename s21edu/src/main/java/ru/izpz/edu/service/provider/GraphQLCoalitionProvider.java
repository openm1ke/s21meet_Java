package ru.izpz.edu.service.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.service.GraphQLService;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"profile.service.enabled", "api.graphql.enabled"}, havingValue = "true")
public class GraphQLCoalitionProvider implements CoalitionProvider {

    private final GraphQLService graphQLService;

    @Override
    public void refreshCoalitionByLogin(String login) {
        graphQLService.refreshStudentCoalitionByLogin(login);
    }
}
