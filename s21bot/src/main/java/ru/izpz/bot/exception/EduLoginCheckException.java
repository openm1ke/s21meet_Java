package ru.izpz.bot.exception;

import lombok.Getter;
import ru.izpz.dto.model.ErrorResponseDTO;

@Getter
public class EduLoginCheckException extends RuntimeException {
    private final transient ErrorResponseDTO error;

    public EduLoginCheckException(ErrorResponseDTO error) {
        super(error.getMessage());
        this.error = error;
    }

}