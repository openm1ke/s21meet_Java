package ru.izpz.dto;

import java.time.OffsetDateTime;
import lombok.*;

/** Structured error returned when a registration or its verification code expires. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationErrorDto {
  public static final String REGISTRATION_EXPIRED_CODE = "REGISTRATION_EXPIRED";
  public static final String VERIFICATION_CODE_EXPIRED_CODE = "VERIFICATION_CODE_EXPIRED";

  private Integer status;
  private String exceptionUUID;
  private String code;
  private String message;
  private String s21login;
  private OffsetDateTime expiresAt;
}
