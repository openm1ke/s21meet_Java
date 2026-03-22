package ru.izpz.edu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "student_project")
public class StudentProject {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    UUID id;

    String login;
    String userId;
    String goalId;
    String name;
    String description;
    Integer experience;
    String dateTime;
    Integer finalPercentage;
    Integer laboriousness;
    String executionType;
    String goalStatus;
    String courseType;
    String displayedCourseStatus;
    Integer amountAnswers;
    Integer amountMembers;
    Integer amountJoinedMembers;
    Integer amountReviewedAnswers;
    Integer amountCodeReviewMembers;
    Integer amountCurrentCodeReviewMembers;
    String groupName;
    Integer localCourseId;
    Integer sortOrder;
    Boolean snapshot;
    OffsetDateTime updatedAt;
}
