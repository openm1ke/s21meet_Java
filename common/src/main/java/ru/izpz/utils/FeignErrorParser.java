package ru.izpz.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.experimental.UtilityClass;
import ru.izpz.dto.ServiceErrorDto;

@UtilityClass
public class FeignErrorParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public ServiceErrorDto parse(FeignException ex) {
        try {
            String content = ex.contentUTF8();
            return mapper.readValue(content, ServiceErrorDto.class);
        } catch (Exception e) {
            return new ServiceErrorDto()
                    .setStatus(ex.status())
                    .setMessage("Ошибка без тела или парсинг не удался");
        }
    }
}
