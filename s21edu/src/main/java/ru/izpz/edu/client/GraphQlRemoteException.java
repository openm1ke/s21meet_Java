package ru.izpz.edu.client;

public class GraphQlRemoteException extends RuntimeException {
    public GraphQlRemoteException(String message) {
        super(message);
    }

    public GraphQlRemoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
