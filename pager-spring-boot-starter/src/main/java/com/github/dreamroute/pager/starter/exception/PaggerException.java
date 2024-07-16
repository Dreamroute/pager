package com.github.dreamroute.pager.starter.exception;

/**
 * 异常
 *
 * @author w.dehi
 */
public class PaggerException extends RuntimeException {
    public PaggerException() {
    }

    public PaggerException(String message) {
        super(message);
    }

    public PaggerException(String message, Throwable cause) {
        super(message, cause);
    }
}
