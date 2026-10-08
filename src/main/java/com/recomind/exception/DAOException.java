package com.recomind.exception;

public class DAOException extends RuntimeException {
    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }
    public DAOException(String message) {
        super(message);
    }
    public DAOException(Throwable cause) {
        super(cause);
    }
}
