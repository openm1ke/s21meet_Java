package ru.izpz.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Data
public class ProfileCodeResponse {
    private String s21login;
    private String secretCode;
    private OffsetDateTime expiresAt;
}
