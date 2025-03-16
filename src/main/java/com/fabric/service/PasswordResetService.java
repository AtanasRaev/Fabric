package com.fabric.service;

public interface PasswordResetService {
    void createPasswordResetToken(String email);

    boolean resetPassword(String token, String newPassword);

    boolean validatePasswordResetToken(String token);
}
