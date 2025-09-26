package ru.izpz.edu.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.GraphQLApiClient;

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
        GData data = client.execute(
                "getCampusPlanOccupied",
                Map.of("clusterId", clusterId),
                QUERY,
                GData.class
        );

        var places = Optional.ofNullable(data)
                .map(GData::student)
                .map(GStudent::getClusterPlanStudentsByClusterId)
                .map(GCluster::occupiedPlaces)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GData(GStudent student) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GStudent(GCluster getClusterPlanStudentsByClusterId) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GCluster(List<GPlace> occupiedPlaces) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GPlace(
            String row, Integer number,
            String stageGroupName, String stageName, String studentType,
            GUser user, GExp experience
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GUser(String id, String login) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GExp(Integer value, GLevel level) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GLevel(GRange range) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GRange(Integer levelCode) {}
}
