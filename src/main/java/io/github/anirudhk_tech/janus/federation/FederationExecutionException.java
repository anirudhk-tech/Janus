package io.github.anirudhk_tech.janus.federation;

public class FederationExecutionException extends RuntimeException {
    
    public FederationExecutionException(String message) {
        super(message);
    }

    public FederationExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
