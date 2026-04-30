package ru.izpz.dto;

import java.util.List;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusResponse {
  private String campusName;
  private List<Clusters> clusters;
  private Map<String, Long> programStats;
}
