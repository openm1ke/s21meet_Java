package ru.izpz.edu.service.provider;

/** Provider for coalition refresh logic. */
public interface CoalitionProvider {

  /** Refresh coalition data for participant login and persist it. */
  void refreshCoalitionByLogin(String login);
}
