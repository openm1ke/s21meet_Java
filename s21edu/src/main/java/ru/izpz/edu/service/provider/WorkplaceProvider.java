package ru.izpz.edu.service.provider;

import ru.izpz.dto.ApiException;
import ru.izpz.edu.model.Workplace;

import java.util.List;

/**
 * Interface for fetching workplace data from configured source.
 */
public interface WorkplaceProvider {

    /**
     * Fetch participants for a cluster using the configured data source.
     * @param clusterId the cluster ID
     * @throws ApiException if operation fails
     */
    List<Workplace> fetchParticipantsByCluster(Long clusterId) throws ApiException;
}
