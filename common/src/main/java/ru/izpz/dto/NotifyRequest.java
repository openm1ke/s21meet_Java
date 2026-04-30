package ru.izpz.dto;

import java.util.List;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyRequest {
  private List<StatusChange> changes;
}
