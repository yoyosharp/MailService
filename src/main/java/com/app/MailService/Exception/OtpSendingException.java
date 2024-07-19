package com.app.MailService.Exception;

public class OtpSendingException extends RuntimeException {
    public OtpSendingException() {
        super();
    }

    public OtpSendingException(String message) {
        super(message);
    }

}
