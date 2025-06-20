package ru.izpz.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clusters {
    private String name;
    private Integer capacity;
    private Integer availableCapacity;
    private Integer floor;
}
