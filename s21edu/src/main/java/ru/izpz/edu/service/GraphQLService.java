package ru.izpz.edu.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.GraphQLApiClient;
import ru.izpz.edu.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "graphql.api.enabled", havingValue = "true")
public class GraphQLService {

    private final GraphQLApiClient client;

    private static final String QUERY = """
        query getCampusPlanOccupied($clusterId: ID!) {
          student {
            getClusterPlanStudentsByClusterId(clusterId: $clusterId) {
              occupiedPlaces {
                row
                number
                stageGroupName
                stageName
                user { id login avatarUrl __typename }
                experience { id value level { id range { id levelCode leftBorder rightBorder __typename } __typename } __typename }
                studentType
                __typename
              }
              __typename
            }
            __typename
          }
        }
        """;

    private static final String GET_CREDENTIALS_QUERY = """
        query getCredentialsByLogin($login: String!) {
          school21 {
            getStudentByLogin(login: $login) {
              studentId
              userId
              schoolId
              isActive
              isGraduate
              __typename
            }
            __typename
          }
        }
        """;

    private static final String GET_STUDENT_PROJECTS_QUERY = """
        query getStudentCurrentProjects($userId: ID!) {
          student {
            getStudentCurrentProjects(userId: $userId) {
              ...StudentProjectItem
              __typename
            }
            __typename
          }
        }
        
        fragment StudentProjectItem on StudentItem {
          goalId
          name
          description
          experience
          dateTime
          finalPercentage
          laboriousness
          executionType
          goalStatus
          courseType
          displayedCourseStatus
          amountAnswers
          amountMembers
          amountJoinedMembers
          amountReviewedAnswers
          amountCodeReviewMembers
          amountCurrentCodeReviewMembers
          groupName
          localCourseId
          __typename
        }
        """;

    public GraphQLService(GraphQLApiClient client) {
        this.client = client;
    }

    public record ClusterSeat(
            String clusterId, String row, int number,
            String login,
            Integer expValue, Integer levelCode,
            String stageGroupName, String stageName
    ) {}

    public List<ClusterSeat> getOccupiedSeats(String clusterId) {
        GraphQLClusterDto data = client.execute(
                "getCampusPlanOccupied",
                Map.of("clusterId", clusterId),
                QUERY,
                GraphQLClusterDto.class
        );

        var places = Optional.ofNullable(data)
                .map(GraphQLClusterDto::student)
                .map(GraphQLStudentDto::getClusterPlanStudentsByClusterId)
                .map(GraphQLClusterDataDto::occupiedPlaces)
                .orElse(List.of());

        return places.stream()
                .map(p -> new ClusterSeat(
                        clusterId,
                        p.row(),
                        p.number() == null ? 0 : p.number(),
                        p.user() == null ? null : p.user().login(),
                        p.experience() == null ? null : p.experience().value(),
                        (p.experience() != null && p.experience().level() != null && p.experience().level().range() != null)
                                ? p.experience().level().range().levelCode() : null,
                        p.stageGroupName(),
                        p.stageName()
                ))
                .toList();
    }

    public String getUserIdByLogin(String login) {
        GraphQLStudentCredentialsDataDto data = client.execute(
                "getCredentialsByLogin",
                Map.of("login", login),
                GET_CREDENTIALS_QUERY,
                GraphQLStudentCredentialsDataDto.class
        );

        return Optional.ofNullable(data)
                .map(GraphQLStudentCredentialsDataDto::school21)
                .map(GraphQLSchool21Dto::getStudentByLogin)
                .map(GraphQLStudentCredentialsDto::userId)
                .orElse(null);
    }

    public List<GraphQLStudentProject> getStudentProjectsByLogin(String login) {
        String userId = getUserIdByLogin(login);
        if (userId == null) {
            return List.of();
        }

        GraphQLStudentProjectsDataDto data = client.execute(
                "getStudentCurrentProjects",
                Map.of("userId", userId),
                GET_STUDENT_PROJECTS_QUERY,
                GraphQLStudentProjectsDataDto.class
        );

        var projects = Optional.ofNullable(data)
                .map(GraphQLStudentProjectsDataDto::student)
                .map(GraphQLStudentQueriesDto::getStudentCurrentProjects)
                .orElse(List.of());

        return projects.stream()
                .map(p -> new GraphQLStudentProject(
                        p.goalId(),
                        p.name(),
                        p.description(),
                        p.experience(),
                        p.dateTime(),
                        p.finalPercentage(),
                        p.laboriousness(),
                        p.executionType(),
                        p.goalStatus(),
                        p.courseType(),
                        p.displayedCourseStatus(),
                        p.amountAnswers(),
                        p.amountMembers(),
                        p.amountJoinedMembers(),
                        p.amountReviewedAnswers(),
                        p.amountCodeReviewMembers(),
                        p.amountCurrentCodeReviewMembers(),
                        p.groupName(),
                        p.localCourseId()
                ))
                .toList();
    }
}
