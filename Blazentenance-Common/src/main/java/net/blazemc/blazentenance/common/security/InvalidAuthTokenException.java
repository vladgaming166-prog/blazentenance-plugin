package net.blazemc.blazentenance.common.security;

public final class InvalidAuthTokenException extends Exception {

    public InvalidAuthTokenException(String message) {
        super(message);
    }

    public InvalidAuthTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
