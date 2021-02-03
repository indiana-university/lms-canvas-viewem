package edu.iu.uits.lms.viewem.controller;

public class DataTooBigException extends RuntimeException {
    public DataTooBigException() {
        super();
    }

    public DataTooBigException(Throwable cause) {
        super(cause);
    }

    public DataTooBigException(String message) {
        super(message);
    }

    public DataTooBigException(String message, Throwable cause) {
        super(message, cause);
    }

    protected DataTooBigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
