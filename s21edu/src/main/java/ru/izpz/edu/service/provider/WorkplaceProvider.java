package ru.izpz.edu.service.provider;

import ru.izpz.dto.ApiException;

/**
 * Simple interface for workplace data management
 * Implementations decide which data source to use internally
 */
public interface WorkplaceProvider {
    
    /**
     * Update participants for a cluster using the configured data source
     * @param clusterId the cluster ID
     * @throws ApiException if operation fails
     */
    void updateParticipantsByCluster(Long clusterId) throws ApiException;
}
