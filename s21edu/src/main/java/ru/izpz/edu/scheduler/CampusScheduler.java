package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.service.CampusService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.scheduler.enabled", havingValue = "true")
public class CampusScheduler {

    private final CampusService campusService;
    private final ClusterRepository clusterRepository;

    @Scheduled(fixedDelayString = "${campus.scheduler.fixed-delay:PT30S}")
    public void parseMskKznNsk() {
        // список целевых кампусов
        List<String> campuses = List.of(
                "6bfe3c56-0211-4fe1-9e59-51616caac4dd", // MSK
                "7c293c9c-f28c-4b10-be29-560e4b000a34", // KZN
                "46e7d965-21e9-4936-bea9-f5ea0d1fddf2"  // NSK
        );

        log.info("Получение кластеров для Москвы, Казани и Новосибирска");
        campuses.parallelStream().forEach(campus -> {
            try {
                campusService.getClustersByCampus(UUID.fromString(campus));
            } catch (ApiException e) {
                log.error("Ошибка получения кластеров для кампуса {}", campus);
            }
        });

        List<Cluster> clusters = clusterRepository.findAll();

        clusters.parallelStream().forEach(cluster -> {
            try {
                campusService.getParticipantsByCluster(cluster.getClusterId());
            } catch (ApiException e) {
                log.error("Ошибка получения участников для кластера {}", cluster.getClusterId());
            }
        });

        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
    }
}
