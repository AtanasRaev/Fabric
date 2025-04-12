package com.fabric.exceptions;

public class ImageUploadFailedException extends RuntimeException {
    public ImageUploadFailedException(String message) {
        super(message);
    }

    public ImageUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
