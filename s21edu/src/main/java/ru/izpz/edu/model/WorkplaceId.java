package ru.izpz.edu.model;

import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceId implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long clusterId;
  private String row;
  private Integer number;
}
