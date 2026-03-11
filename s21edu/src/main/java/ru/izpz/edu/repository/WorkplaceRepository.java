package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, WorkplaceId> {
    void deleteByIdClusterId(Long clusterId);
    void deleteByIdClusterIdIn(Set<Long> clusterIds);

    boolean existsByLogin(String login);

    @Query("select distinct w.login from Workplace w")
    List<String> findDistinctLogins();

    Optional<Workplace> findByLogin(String telegramId);

    List<Workplace> findAllByLoginIn(Collection<String> logins);

    @Query("""
        select c.campusId as campusId, count(w) as count
        from Workplace w
        join Cluster c on c.clusterId = w.id.clusterId
        group by c.campusId
        """)
    List<CampusCountView> countParticipantsByCampus();

    @Query("""
        select c.campusId as campusId, w.stageGroupName as stageGroupName, count(w) as count
        from Workplace w
        join Cluster c on c.clusterId = w.id.clusterId
        group by c.campusId, w.stageGroupName
        """)
    List<CampusStageGroupCountView> countParticipantsByCampusAndStageGroup();

    @Query("""
        select c.campusId as campusId, w.stageName as stageName, count(w) as count
        from Workplace w
        join Cluster c on c.clusterId = w.id.clusterId
        group by c.campusId, w.stageName
        """)
    List<CampusStageNameCountView> countParticipantsByCampusAndStageName();

    interface CampusCountView {
        String getCampusId();
        long getCount();
    }

    interface CampusStageGroupCountView {
        String getCampusId();
        String getStageGroupName();
        long getCount();
    }

    interface CampusStageNameCountView {
        String getCampusId();
        String getStageName();
        long getCount();
    }
}
