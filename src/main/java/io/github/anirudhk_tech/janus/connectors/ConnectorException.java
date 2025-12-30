package io.github.anirudhk_tech.janus.connectors;

public class ConnectorException extends RuntimeException {
    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}


