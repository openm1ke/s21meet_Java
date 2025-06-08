package ru.izpz.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.experimental.UtilityClass;
import ru.izpz.dto.model.ErrorResponseDTO;

@UtilityClass
public class FeignErrorParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public ErrorResponseDTO parse(FeignException ex) {
        try {
            String content = ex.contentUTF8();
            return mapper.readValue(content, ErrorResponseDTO.class);
        } catch (Exception e) {
            ErrorResponseDTO fallback = new ErrorResponseDTO();
            fallback.setStatus(ex.status());
            fallback.setMessage("Ошибка без тела или парсинг не удался");
            return fallback;
        }
    }
}
