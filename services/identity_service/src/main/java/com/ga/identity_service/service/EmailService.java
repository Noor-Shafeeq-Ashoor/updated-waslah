
package com.ga.identity_service.service;

/**
 * Defines the email operations supported by WASLAH.
 */
public interface EmailService {

    void sendVerificationEmail(String email, String verificationLink);

    void sendPasswordResetEmail(String email, String resetLink);
}
