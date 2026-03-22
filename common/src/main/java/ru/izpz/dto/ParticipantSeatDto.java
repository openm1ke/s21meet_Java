package ru.izpz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSeatDto {
    private String clusterName;
    private String row;
    private Integer number;
    private String stageGroupName;
    private String stageName;
}
