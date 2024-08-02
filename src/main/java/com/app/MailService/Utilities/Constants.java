package com.app.MailService.Utilities;

import java.util.List;

public class Constants {

    public static final String OTP_STATUS_PENDING = "pending";
    public static final String OTP_STATUS_EXPIRED = "expired";
    public static final String OTP_STATUS_VERIFIED = "verified";
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
    public static final String OTP_EMAIL_SUBJECT = "Mã xác thực OTP";

    public static final String EXCHANGE_NAME = "mailServiceExchange";
    public static final String REGISTER_OTP_QUEUE = "registerQueue";
    public static final String LOGIN_OTP_QUEUE = "loginQueue";
    public static final String OTP_CARD_QUEUE = "otpCardQueue";
    public static final String FORGOT_PASSWORD_OTP_QUEUE = "forgotPasswordQueue";
    public static final String REGISTER_OTP_ROUTING_KEY = "register";
    public static final String LOGIN_OTP_ROUTING_KEY = "login";
    public static final String FORGOT_PASSWORD_OTP_ROUTING_KEY = "forgotPassword";
    public static final String OTP_CARD_QUEUE_ROUTING_KEY = "otpCard";
    public static final List<String> routingKeys = List.of(
            REGISTER_OTP_ROUTING_KEY,
            LOGIN_OTP_ROUTING_KEY,
            FORGOT_PASSWORD_OTP_ROUTING_KEY,
            OTP_CARD_QUEUE_ROUTING_KEY
    );

    public static final String OTP_CARD_STATUS_ACTIVE = "active";
    public static final String OTP_CARD_STATUS_DEACTIVATED = "deactivate";
    public static final String OTP_CARD_STATUS_AVAILABLE = "available";
    public static final String OTP_CARD_STATUS_LOCKED = "locked";
    public static final String OTP_CARD_TEMPLATE_NAME = "otp_card";

    public static final List<String> OTP_CARD_STATUS_CHANGEABLE_FROM_ACTIVE = List.of(
            OTP_CARD_STATUS_DEACTIVATED,
            OTP_CARD_STATUS_LOCKED
    );

    public static final List<String> OTP_CARD_STATUS_CHANGEABLE_FROM_LOCKED = List.of(
            OTP_CARD_STATUS_ACTIVE,
            OTP_CARD_STATUS_DEACTIVATED
    );

    public static final String OTP_CARD_SEND_EMAIL_FROM_ADDRESS = "noreply@example.com";
    public static final String OTP_CARD_SEND_EMAIL_SENDER_NAME = "Thông báo từ hệ thống";
    public static final String OTP_CARD_EMAIL_SUBJECT = "Yêu cầu tạo OTP Card thành công";
    public static final String OTP_CARD_EMAIL_TEMPLATE = "otp_card_pdf";
}

