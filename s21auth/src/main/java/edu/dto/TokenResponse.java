package edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private int expires_in;
}
