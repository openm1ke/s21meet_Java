package edu.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenRequest {
    private String login;
    private String password;
}
