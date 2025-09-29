package ru.izpz.edu.repository;

import ru.izpz.dto.ParticipantStatusEnum;

public interface ParticipantView {
    String getLogin();
    ParticipantStatusEnum getStatus();
}
