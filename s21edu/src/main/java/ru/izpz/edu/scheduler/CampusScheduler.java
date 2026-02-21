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
import ru.izpz.edu.model.Cluster;

import java.util.List;
import java.util.concurrent.ExecutorService;
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
        List<String> campuses = getTargetCampuses();
        
        log.info("Получение кластеров для Москвы, Казани и Новосибирска");
        StopWatch stopWatch = new StopWatch("campus");
        
        processClusters(campuses, stopWatch);
        processParticipants(stopWatch);
        
        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
        log.info("Время обновления: {}", stopWatch.prettyPrint());
    }

    private List<String> getTargetCampuses() {
        return List.of(
            "6bfe3c56-0211-4fe1-9e59-51616caac4dd", // MSK
            "7c293c9c-f28c-4b10-be29-560e4b000a34", // KZN
            "46e7d965-21e9-4936-bea9-f5ea0d1fddf2"  // NSK
        );
    }

    private void processClusters(List<String> campuses, StopWatch stopWatch) {
        stopWatch.start("get clusters");
        try (var vexec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<? extends Future<?>> futures = submitClusterTasks(campuses, vexec);
            waitForClusterTasksCompletion(futures);
        }
        stopWatch.stop();
    }

    private List<? extends Future<?>> submitClusterTasks(List<String> campuses, ExecutorService vexec) {
        return campuses.stream()
            .map(id -> vexec.submit(() -> processSingleCampus(id)))
            .toList();
    }

    private void processSingleCampus(String id) {
        try {
            var clusters = campusClient.getClustersByCampus(id);
            campusService.replaceClustersByCampusId(id, clusters);
        } catch (ApiException e) {
            log.error("Ошибка получения кластеров для кампуса {}", id, e);
        }
    }

    private void waitForClusterTasksCompletion(List<? extends Future<?>> futures) {
        for (var f : futures) {
            try { 
                f.get(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Прервано получение кластеров для кампуса", e);
                return;
            } catch (Exception e) { 
                log.error("Ошибка получение кластеров для кампуса", e); 
            }
        }
    }

    private void processParticipants(StopWatch stopWatch) {
        stopWatch.start("get participants");
        try (var vexec = Executors.newVirtualThreadPerTaskExecutor()) {
            var clusterList = campusService.findAllByOrderByCampusIdAsc();
            List<? extends Future<?>> futures = submitParticipantTasks(clusterList, vexec);
            waitForParticipantTasksCompletion(futures);
        }
        stopWatch.stop();
    }

    private List<? extends Future<?>> submitParticipantTasks(List<Cluster> clusterList, ExecutorService vexec) {
        return clusterList.stream()
            .map(c -> vexec.submit(() -> processSingleCluster(c)))
            .toList();
    }

    private void processSingleCluster(Cluster c) {
        long cid = c.getClusterId();
        try {
            campusService.replaceParticipantsByClusterIdWithProvider(cid);
            log.info("Updated participants for cluster {} ({})", c.getName(), cid);
        } catch (ApiException e) {
            log.error("Ошибка получения участников для кластера {}", cid, e);
        }
    }

    private void waitForParticipantTasksCompletion(List<? extends Future<?>> futures) {
        for (Future<?> f : futures) {
            try { 
                f.get(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Прервано получение занятых мест в кластере", e);
                return;
            } catch (Exception e) { 
                log.error("Ошибка получения занятых мест в кластере", e); 
            }
        }
    }
}
