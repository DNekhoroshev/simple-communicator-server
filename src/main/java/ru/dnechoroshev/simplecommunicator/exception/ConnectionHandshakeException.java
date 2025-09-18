package ru.dnechoroshev.simplecommunicator.exception;

public class ConnectionHandshakeException extends RuntimeException {
    public ConnectionHandshakeException() {
        super();
    }

    public ConnectionHandshakeException(String message) {
        super(message);
    }

    public ConnectionHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
