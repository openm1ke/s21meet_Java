package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.dto.GraphQLStudentProject;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentProjectRefreshServiceTest {

    @Mock
    private StudentProjectRepository studentProjectRepository;

    @Test
    void replaceProjects_shouldStoreSnapshot_whenProjectsEmpty() {
        StudentProjectRefreshService service = new StudentProjectRefreshService(studentProjectRepository);

        service.replaceProjects("login", "userId", List.of());

        verify(studentProjectRepository).deleteByLogin("login");
        verify(studentProjectRepository).save(argThat(p ->
                Boolean.TRUE.equals(p.getSnapshot())
                        && Integer.valueOf(0).equals(p.getSortOrder())
                        && "login".equals(p.getLogin())
                        && "userId".equals(p.getUserId())
                        && p.getUpdatedAt() != null
        ));
        verify(studentProjectRepository, never()).saveAll(any());
    }

    @Test
    void replaceProjects_shouldStoreMappedProjects_whenProjectsExist() {
        StudentProjectRefreshService service = new StudentProjectRefreshService(studentProjectRepository);
        GraphQLStudentProject p1 = new GraphQLStudentProject(
                "g1", "Project1", "d1", 100, "2026-01-01", 10, 1,
                "INDIVIDUAL", "IN_PROGRESS", "CORE", "IN_PROGRESS",
                1, 2, 3, 4, 5, 6, "grp1", 7
        );
        GraphQLStudentProject p2 = new GraphQLStudentProject(
                "g2", "Project2", "d2", 200, "2026-01-02", 20, 2,
                "GROUP", "WAITING_FOR_START", "CORE", "WAITING_FOR_START",
                2, 3, 4, 5, 6, 7, "grp2", 8
        );

        service.replaceProjects("login", "userId", List.of(p1, p2));

        verify(studentProjectRepository).deleteByLogin("login");
        verify(studentProjectRepository).saveAll(argThat(iterable -> {
            List<StudentProject> list = (iterable instanceof List<StudentProject> l) ? l : List.of();
            return list.size() == 2
                    && "g1".equals(list.get(0).getGoalId())
                    && "g2".equals(list.get(1).getGoalId())
                    && Integer.valueOf(0).equals(list.get(0).getSortOrder())
                    && Integer.valueOf(1).equals(list.get(1).getSortOrder())
                    && !Boolean.TRUE.equals(list.get(0).getSnapshot())
                    && !Boolean.TRUE.equals(list.get(1).getSnapshot())
                    && list.get(0).getUpdatedAt() != null
                    && list.get(1).getUpdatedAt() != null;
        }));
    }
}
