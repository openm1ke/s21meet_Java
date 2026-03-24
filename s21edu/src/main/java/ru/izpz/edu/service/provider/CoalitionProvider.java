package ru.izpz.edu.service.provider;

import ru.izpz.dto.ApiException;

/**
 * Provider for coalition refresh logic.
 */
public interface CoalitionProvider {

    /**
     * Refresh coalition data for participant login and persist it.
     */
    void refreshCoalitionByLogin(String login) throws ApiException;
}
