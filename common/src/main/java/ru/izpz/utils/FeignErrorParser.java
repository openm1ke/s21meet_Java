package ru.izpz.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.experimental.UtilityClass;
import ru.izpz.dto.RegistrationErrorDto;
import ru.izpz.dto.ServiceErrorDto;

@UtilityClass
public class FeignErrorParser {

  private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

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

  public RegistrationErrorDto parseRegistrationError(FeignException ex) {
    RegistrationErrorDto fallback = new RegistrationErrorDto();
    fallback.setStatus(ex.status());
    fallback.setMessage(ex.contentUTF8());
    try {
      RegistrationErrorDto parsed = mapper.readValue(ex.contentUTF8(), RegistrationErrorDto.class);
      if (parsed != null) {
        return parsed;
      }
    } catch (Exception e) {
      return fallback;
    }
    return fallback;
  }

  public boolean isRegistrationExpired(RegistrationErrorDto error, FeignException exception) {
    if (error != null
        && (RegistrationErrorDto.REGISTRATION_EXPIRED_CODE.equals(error.getCode())
            || RegistrationErrorDto.VERIFICATION_CODE_EXPIRED_CODE.equals(error.getCode()))) {
      return true;
    }
    String responseBody = exception.contentUTF8();
    return responseBody.contains("Попытка регистрации истекла")
        || responseBody.contains("Код подтверждения истек");
  }
}
