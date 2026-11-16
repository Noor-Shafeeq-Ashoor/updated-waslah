package com.ga.notification_service.messaging;

public class InvalidNotificationEventException extends RuntimeException{

    public InvalidNotificationEventException(String message) {
        super(message);
    }

    public InvalidNotificationEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
