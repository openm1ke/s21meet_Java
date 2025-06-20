package ru.izpz.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusResponse {
    private String campusName;
    private List<Clusters> clusters;
}
