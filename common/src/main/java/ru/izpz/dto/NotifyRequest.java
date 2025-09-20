package ru.izpz.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyRequest {
    private List<StatusChange> changes;
}
