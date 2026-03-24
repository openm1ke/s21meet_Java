package ru.izpz.edu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class StudentProjectRefreshService {

    private final StudentProjectRepository studentProjectRepository;

    public StudentProjectRefreshService(StudentProjectRepository studentProjectRepository) {
        this.studentProjectRepository = studentProjectRepository;
    }

    @Transactional
    public void replaceProjects(String login, String userId, List<StudentProjectData> projects) {
        OffsetDateTime now = OffsetDateTime.now();
        studentProjectRepository.deleteByLogin(login);
        if (projects.isEmpty()) {
            studentProjectRepository.save(toSnapshotEntity(login, userId, now));
            return;
        }

        List<StudentProject> entities = IntStream.range(0, projects.size())
                .mapToObj(i -> toEntity(login, userId, projects.get(i), now, i))
                .toList();
        studentProjectRepository.saveAll(entities);
    }

    private StudentProject toEntity(String login,
                                    String userId,
                                    StudentProjectData project,
                                    OffsetDateTime updatedAt,
                                    int sortOrder) {
        StudentProject entity = new StudentProject();
        entity.setLogin(login);
        entity.setUserId(userId);
        entity.setGoalId(project.goalId());
        entity.setName(project.name());
        entity.setDescription(project.description());
        entity.setExperience(project.experience());
        entity.setDateTime(project.dateTime());
        entity.setFinalPercentage(project.finalPercentage());
        entity.setLaboriousness(project.laboriousness());
        entity.setExecutionType(project.executionType());
        entity.setGoalStatus(project.goalStatus());
        entity.setAmountMembers(project.amountMembers());
        entity.setLocalCourseId(project.localCourseId());
        entity.setSortOrder(sortOrder);
        entity.setSnapshot(false);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private StudentProject toSnapshotEntity(String login, String userId, OffsetDateTime updatedAt) {
        StudentProject entity = new StudentProject();
        entity.setLogin(login);
        entity.setUserId(userId);
        entity.setSortOrder(0);
        entity.setSnapshot(true);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}
