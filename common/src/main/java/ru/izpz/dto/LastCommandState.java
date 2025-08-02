package ru.izpz.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class LastCommandState {
    public LastCommandType command; // enum
    public Map<String, String> args;
}
