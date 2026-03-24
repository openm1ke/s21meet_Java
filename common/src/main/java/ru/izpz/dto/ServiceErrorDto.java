package ru.izpz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ServiceErrorDto {
    private Integer status;
    private String exceptionUUID;
    private String code;
    private String message;
}
