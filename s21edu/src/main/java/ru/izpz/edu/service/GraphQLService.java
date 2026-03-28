package ru.izpz.edu.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.GraphQLApiClient;
import ru.izpz.edu.dto.*;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentCoalitionRepository;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@ConditionalOnProperty(name = "api.graphql.enabled", havingValue = "true")
public class GraphQLService {

    private static final String COALITION_REFRESH_COUNTER = "edu_graphql_coalition_refresh_total";
    private static final String COALITION_REFRESH_DURATION = "edu_graphql_coalition_refresh_duration_seconds";
    private static final String PROJECTS_REFRESH_COUNTER = "edu_graphql_projects_refresh_total";
    private static final String PROJECTS_REFRESH_DURATION = "edu_graphql_projects_refresh_duration_seconds";
    private static final String TAG_OUTCOME = "outcome";
    private static final String USER_ID_PARAM = "userId";
    private final GraphQLApiClient client;
    private final StudentCredentialsRepository studentCredentialsRepository;
    private final StudentCoalitionRepository studentCoalitionRepository;
    private final StudentProjectRepository studentProjectRepository;
    private final StudentProjectRefreshService studentProjectRefreshService;
    private final MeterRegistry meterRegistry;
    private Duration coalitionRefreshTtl = Duration.ofMinutes(15);
    private Duration projectsRefreshTtl = Duration.ofMinutes(15);

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

    private static final String GET_STUDENT_COALITION_QUERY = """
        query publicProfileGetCoalition($userId: UUID!) {
          student {
            getUserTournamentWidget(userId: $userId) {
              coalitionMember {
                coalition {
                  avatarUrl
                  color
                  name
                  memberCount
                  __typename
                }
                currentTournamentPowerRank {
                  rank
                  power {
                    id
                    points
                    __typename
                  }
                  __typename
                }
                __typename
              }
              lastTournamentResult {
                userRank
                power
                __typename
              }
              __typename
            }
            __typename
          }
        }
        """;

    public GraphQLService(GraphQLApiClient client,
                          StudentCredentialsRepository studentCredentialsRepository,
                          StudentCoalitionRepository studentCoalitionRepository,
                          StudentProjectRepository studentProjectRepository,
                          StudentProjectRefreshService studentProjectRefreshService,
                          MeterRegistry meterRegistry) {
        this.client = client;
        this.studentCredentialsRepository = studentCredentialsRepository;
        this.studentCoalitionRepository = studentCoalitionRepository;
        this.studentProjectRepository = studentProjectRepository;
        this.studentProjectRefreshService = studentProjectRefreshService;
        this.meterRegistry = meterRegistry;
    }

    @Value("${coalition.refresh-ttl:PT15M}")
    void setCoalitionRefreshTtl(Duration coalitionRefreshTtl) {
        this.coalitionRefreshTtl = coalitionRefreshTtl;
    }

    @Value("${projects.refresh-ttl:PT15M}")
    void setProjectsRefreshTtl(Duration projectsRefreshTtl) {
        this.projectsRefreshTtl = projectsRefreshTtl;
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

    public GraphQLStudentCredentialsDto getStudentCredentialsByLogin(String login) {
        return studentCredentialsRepository.findById(login)
                .map(this::toDto)
                .orElseGet(() -> fetchAndStoreCredentials(login));
    }

    public String getUserIdByLogin(String login) {
        return Optional.ofNullable(getStudentCredentialsByLogin(login))
                .map(GraphQLStudentCredentialsDto::userId)
                .orElse(null);
    }

    public List<StudentProjectData> getStudentProjectsByLogin(String login) {
        String userId = getUserIdByLogin(login);
        if (userId == null) {
            return List.of();
        }

        GraphQLStudentProjectsDataDto data = client.execute(
                "getStudentCurrentProjects",
                Map.of(USER_ID_PARAM, userId),
                GET_STUDENT_PROJECTS_QUERY,
                GraphQLStudentProjectsDataDto.class
        );

        var projects = Optional.ofNullable(data)
                .map(GraphQLStudentProjectsDataDto::student)
                .map(GraphQLStudentQueriesDto::getStudentCurrentProjects)
                .orElse(List.of());

        return projects.stream()
                .map(p -> new StudentProjectData(
                        p.goalId(),
                        p.name(),
                        p.description(),
                        p.experience(),
                        p.dateTime(),
                        p.finalPercentage(),
                        p.laboriousness(),
                        p.executionType(),
                        p.goalStatus(),
                        p.amountMembers(),
                        p.localCourseId()
                ))
                .toList();
    }

    public List<StudentProjectData> getCachedStudentProjectsByLogin(String login) {
        try {
            refreshStudentProjectsByLogin(login);
        } catch (RuntimeException e) {
            log.warn("Не удалось обновить данные проектов для {}: {}", login, e.getMessage(), e);
        }

        return studentProjectRepository.findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(login).stream()
                .map(this::toDto)
                .toList();
    }

    public void refreshStudentProjectsByLogin(String login) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            if (!isProjectsRefreshRequired(login)) {
                outcome = "skipped_ttl";
                return;
            }

            String userId = getUserIdByLogin(login);
            if (userId == null) {
                outcome = "skipped_no_user_id";
                return;
            }

            GraphQLStudentProjectsDataDto data = client.execute(
                    "getStudentCurrentProjects",
                    Map.of(USER_ID_PARAM, userId),
                    GET_STUDENT_PROJECTS_QUERY,
                    GraphQLStudentProjectsDataDto.class
            );

            var projects = Optional.ofNullable(data)
                    .map(GraphQLStudentProjectsDataDto::student)
                    .map(GraphQLStudentQueriesDto::getStudentCurrentProjects)
                    .orElse(List.of());

            studentProjectRefreshService.replaceProjects(login, userId, projects);
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            meterRegistry.counter(PROJECTS_REFRESH_COUNTER, TAG_OUTCOME, outcome).increment();
            sample.stop(Timer.builder(PROJECTS_REFRESH_DURATION)
                    .description("Duration of GraphQL student projects refresh requests")
                    .tag(TAG_OUTCOME, outcome)
                    .register(meterRegistry));
        }
    }

    public void refreshStudentCoalitionByLogin(String login) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            if (!isCoalitionRefreshRequired(login)) {
                outcome = "skipped_ttl";
                return;
            }

            String userId = getUserIdByLogin(login);
            if (userId == null) {
                outcome = "skipped_no_user_id";
                return;
            }

            GraphQLStudentCoalitionDataDto data = client.execute(
                    "publicProfileGetCoalition",
                    Map.of(USER_ID_PARAM, userId),
                    GET_STUDENT_COALITION_QUERY,
                    GraphQLStudentCoalitionDataDto.class
            );

            String coalitionName = Optional.ofNullable(data)
                    .map(GraphQLStudentCoalitionDataDto::student)
                    .map(GraphQLStudentTournamentDataDto::getUserTournamentWidget)
                    .map(GraphQLUserTournamentWidgetDto::coalitionMember)
                    .map(GraphQLCoalitionMemberDto::coalition)
                    .map(GraphQLCoalitionDataDto::name)
                    .orElse(null);

            Integer coalitionMembers = Optional.ofNullable(data)
                    .map(GraphQLStudentCoalitionDataDto::student)
                    .map(GraphQLStudentTournamentDataDto::getUserTournamentWidget)
                    .map(GraphQLUserTournamentWidgetDto::coalitionMember)
                    .map(GraphQLCoalitionMemberDto::coalition)
                    .map(GraphQLCoalitionDataDto::memberCount)
                    .orElse(null);

            Integer coalitionRank = Optional.ofNullable(data)
                    .map(GraphQLStudentCoalitionDataDto::student)
                    .map(GraphQLStudentTournamentDataDto::getUserTournamentWidget)
                    .map(GraphQLUserTournamentWidgetDto::coalitionMember)
                    .map(GraphQLCoalitionMemberDto::currentTournamentPowerRank)
                    .map(GraphQLCurrentTournamentPowerRankDto::rank)
                    .orElse(null);

            StudentCoalition entity = studentCoalitionRepository.findById(login)
                    .orElseGet(StudentCoalition::new);
            entity.setLogin(login);
            entity.setUserId(userId);
            entity.setCoalitionName(coalitionName);
            entity.setMemberCount(coalitionMembers);
            entity.setRank(coalitionRank);
            entity.setUpdatedAt(OffsetDateTime.now());
            studentCoalitionRepository.save(entity);
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            meterRegistry.counter(COALITION_REFRESH_COUNTER, TAG_OUTCOME, outcome).increment();
            sample.stop(Timer.builder(COALITION_REFRESH_DURATION)
                    .description("Duration of GraphQL coalition refresh requests")
                    .tag(TAG_OUTCOME, outcome)
                    .register(meterRegistry));
        }
    }

    private boolean isCoalitionRefreshRequired(String login) {
        return studentCoalitionRepository.findById(login)
                .map(StudentCoalition::getUpdatedAt)
                .map(updatedAt -> updatedAt.isBefore(OffsetDateTime.now().minus(coalitionRefreshTtl)))
                .orElse(true);
    }

    private boolean isProjectsRefreshRequired(String login) {
        return Optional.ofNullable(studentProjectRepository.findMaxUpdatedAtByLogin(login))
                .map(updatedAt -> updatedAt.isBefore(OffsetDateTime.now().minus(projectsRefreshTtl)))
                .orElse(true);
    }

    private GraphQLStudentCredentialsDto fetchAndStoreCredentials(String login) {
        GraphQLStudentCredentialsDataDto data = client.execute(
                "getCredentialsByLogin",
                Map.of("login", login),
                GET_CREDENTIALS_QUERY,
                GraphQLStudentCredentialsDataDto.class
        );

        return Optional.ofNullable(data)
                .map(GraphQLStudentCredentialsDataDto::school21)
                .map(GraphQLSchool21Dto::getStudentByLogin)
                .map(credentials -> {
                    StudentCredentials entity = new StudentCredentials();
                    entity.setLogin(login);
                    entity.setStudentId(credentials.studentId());
                    entity.setUserId(credentials.userId());
                    entity.setSchoolId(credentials.schoolId());
                    entity.setIsActive(credentials.isActive());
                    entity.setIsGraduate(credentials.isGraduate());
                    studentCredentialsRepository.save(entity);
                    return credentials;
                })
                .orElse(null);
    }

    private GraphQLStudentCredentialsDto toDto(StudentCredentials credentials) {
        return new GraphQLStudentCredentialsDto(
                credentials.getStudentId(),
                credentials.getUserId(),
                credentials.getSchoolId(),
                credentials.getIsActive(),
                credentials.getIsGraduate()
        );
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

}
