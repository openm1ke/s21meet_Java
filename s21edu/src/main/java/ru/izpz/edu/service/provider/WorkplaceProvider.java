package ru.izpz.edu.service.provider;

import java.util.List;
import ru.izpz.edu.model.Workplace;

/** Interface for fetching workplace data from configured source. */
public interface WorkplaceProvider {

  /**
   * Fetch participants for a cluster using the configured data source.
   *
   * @param clusterId the cluster ID
   */
  List<Workplace> fetchParticipantsByCluster(Long clusterId);
}
