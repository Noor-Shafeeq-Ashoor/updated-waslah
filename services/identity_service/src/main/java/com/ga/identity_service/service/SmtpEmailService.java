
package com.ga.identity_service.service;


import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@Profile("!dev")

public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(
            String email,
            String verificationLink
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Verify your WASLAH account");
        message.setText("Welcome to WASLAH!\n\n"
                        + "Please verify your email using this link:\n"
                        + verificationLink
                        + "\n\nIf you did not create this account, "
                        + "you can ignore this email."
        );

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Reset your WASLAH password");
        message.setText("A password reset was requested for your WASLAH account.\n\n"
                        + "Use this link to reset your password:\n"
                        + resetLink
                        + "\n\nIf you did not request this, "
                        + "you can ignore this email."
        );

        mailSender.send(message);
    }
}
