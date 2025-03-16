package com.fabric.service;

import com.fabric.database.entity.Order;

public interface EmailService {
    void sendOrderEmail(Order order);

    void sendPasswordResetEmail(String email, String resetLink);
}
