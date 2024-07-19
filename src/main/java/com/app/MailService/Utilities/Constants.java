package com.app.MailService.Utilities;

import java.util.List;

public class Constants {

    public static final String OTP_STATUS_PENDING = "pending";
    public static final String OTP_STATUS_EXPIRED = "expired";
    public static final String OTP_STATUS_VALIDATED = "validated";
    public static final String OTP_STATUS_REJECTED = "rejected";

    public static final String OTP_SEND_TYPE_EMAIL = "email";
    public static final String OTP_SEND_TYPE_SMS = "sms";
    public static final String OTP_SEND_TYPE_CARD = "card";
    public static final List<String> OTP_SEND_TYPES = List.of(
            OTP_SEND_TYPE_EMAIL,
            OTP_SEND_TYPE_SMS,
            OTP_SEND_TYPE_CARD
    );

    public static final String OTP_TYPE_REGISTER = "register";
    public static final String OTP_TYPE_LOGIN = "login";
    public static final String OTP_TYPE_FORGOT_PASSWORD = "forgot-password";
    public static final String OTP_TYPE_SHOP_PAYMENT = "shop-payment";
    public static final List<String> OTP_TYPES = List.of(
            OTP_TYPE_REGISTER,
            OTP_TYPE_LOGIN,
            OTP_TYPE_FORGOT_PASSWORD,
            OTP_TYPE_SHOP_PAYMENT
    );

    public static final String GMAIL_BY_JAVA_MAILER = "GmailByJavaMailer";
    public static final String GMAIL_API = "GmailApi";
    public static final String ZEPTO_MAIL = "ZeptoMail";

    public static final String OTP_SEND_EMAIL_FROM_ADDRESS = "noreply@example.com";
    public static final String OTP_SEND_EMAIL_SENDER_NAME = "System Notification";
    public static final String OTP_SEND_EMAIL_SUBJECT = "Mã OTP đăng ký tài khoản";

    public static final String EXCHANGE_NAME = "mailServiceExchange";
    public static final String REGISTER_OTP_QUEUE = "registerQueue";
    public static final String LOGIN_OTP_QUEUE = "loginQueue";
    public static final String FORGOT_PASSWORD_OTP_QUEUE = "forgotPasswordQueue";
    public static final String REGISTER_OTP_ROUTING_KEY = "register";
    public static final String LOGIN_OTP_ROUTING_KEY = "login";
    public static final String FORGOT_PASSWORD_OTP_ROUTING_KEY = "forgotPassword";
    public static final List<String> routingKeys = List.of(
            REGISTER_OTP_ROUTING_KEY,
            LOGIN_OTP_ROUTING_KEY,
            FORGOT_PASSWORD_OTP_ROUTING_KEY
    );
}

