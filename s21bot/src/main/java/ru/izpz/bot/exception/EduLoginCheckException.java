package ru.izpz.bot.exception;

import lombok.Getter;
import ru.izpz.dto.ServiceErrorDto;

@Getter
public class EduLoginCheckException extends RuntimeException {
    private final transient ServiceErrorDto error;

    public EduLoginCheckException(ServiceErrorDto error) {
        super(error.getMessage());
        this.error = error;
    }

}
