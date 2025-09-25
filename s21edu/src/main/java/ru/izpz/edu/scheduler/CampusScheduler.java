package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.service.CampusService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.scheduler.enabled", havingValue = "true")
public class CampusScheduler {

    private final CampusClient campusClient;
    private final CampusService campusService;

    @Scheduled(fixedDelayString = "${campus.scheduler.fixed-delay:PT30S}")
    public void parseMskKznNsk() {
        // список целевых кампусов
        List<String> campuses = List.of(
            "6bfe3c56-0211-4fe1-9e59-51616caac4dd", // MSK
            "7c293c9c-f28c-4b10-be29-560e4b000a34", // KZN
            "46e7d965-21e9-4936-bea9-f5ea0d1fddf2"  // NSK
        );

        log.info("Получение кластеров для Москвы, Казани и Новосибирска");
        StopWatch stopWatch = new StopWatch("campus");
        stopWatch.start("get clusters");
        try (var vexec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<? extends Future<?>> f1 = campuses.stream()
                .map(id -> vexec.submit(() -> {
                    try {
                        var clusters = campusClient.getClustersByCampus(id);
                        campusService.replaceClustersByCampusId(id, clusters);
                        //log.info("Кампус {}: кластеров {}", id, clusters.size());
                    } catch (ApiException e) {
                        log.error("Ошибка получения кластеров для кампуса {}", id, e);
                    }
                }))
                .toList();

            for (var f : f1) {
                try { f.get(); } catch (Exception e) { log.error("Ошибка получение кластеров для кампуса", e); }
            }
            stopWatch.stop();
            stopWatch.start("get participants");
            var clusterList = campusService.findAllByOrderByCampusIdAsc();
            List<? extends Future<?>> f2 = clusterList.stream()
                .map(c -> vexec.submit(() -> {
                    long cid = c.getClusterId();
                    try {
                        var seats = campusClient.getParticipantsByCluster(cid);
                        campusService.replaceParticipantsByClusterId(cid, seats);
                        //log.info("мест: {} в {} ({})", seats.size(), c.getName(), cid);
                    } catch (ru.izpz.dto.ApiException e) {
                        log.error("Ошибка получения участников для кластера {}", cid, e);
                    }
                }))
                .toList();

            for (Future<?> f : f2) {
                try { f.get(); } catch (Exception e) { log.error("Ошибка получения занятых мест в кластере", e); }
            }
        }

        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
        stopWatch.stop();
        log.info("Время обновления: {}", stopWatch.prettyPrint());
    }
}
