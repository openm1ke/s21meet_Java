package ru.izpz.dto;

import java.time.OffsetDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileCodeResponse {
  private String s21login;
  private String secretCode;
  private OffsetDateTime expiresAt;
}
