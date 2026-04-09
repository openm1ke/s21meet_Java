package ru.izpz.edu.service.provider;

import ru.izpz.edu.dto.StudentProjectData;

import java.util.List;

/**
 * Provider for loading participant projects from the configured source.
 */
public interface ProjectsProvider {

    /**
     * Returns current participant projects by login.
     */
    List<StudentProjectData> getStudentProjectsByLogin(String login);

    /**
     * Refreshes cached participant projects by login.
     */
    default void refreshStudentProjectsByLogin(String login) {
        getStudentProjectsByLogin(login);
    }
}
