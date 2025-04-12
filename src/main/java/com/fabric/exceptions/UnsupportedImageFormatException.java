package com.fabric.exceptions;

public class UnsupportedImageFormatException extends RuntimeException {

    public UnsupportedImageFormatException(String message) {
        super(message);
    }

    public UnsupportedImageFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
