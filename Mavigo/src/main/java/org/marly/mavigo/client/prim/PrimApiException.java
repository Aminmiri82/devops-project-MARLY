package org.marly.mavigo.client.prim;

public class PrimApiException extends RuntimeException {

    public PrimApiException(String message) {
        super(message);
    }

    public PrimApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

