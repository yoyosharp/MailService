package com.app.MailService.Utilities;

public class EndPointsConstants {

    public static final String API_V1 = "/api/v1";

    public static final String SEND_MAIL = "/send-mail";
    public static final String API_V1_SEND_MAIL = API_V1 + SEND_MAIL;
    public static final String SEND_MAIL_SINGLE = "/send-single-mail";

    public static final String OTP = "/otp";
    public static final String API_V1_OTP = API_V1 + OTP;
    public static final String GENERATE_OTP = "/generate";
    public static final String VERIFY_OTP = "/verify";
    public static final String RESEND_OTP = "/resend";

    public static final String SHOW_CARDS = "/show-cards";
    public static final String CREATE_CARD = "/create-card";
    public static final String GENERATE_CARDS = "/generate-cards";
    public static final String BIND_CARD = "/bind-card";
    public static final String LOCK_CARD = "/lock-card";
    public static final String UNLOCK_CARD = "/unlock-card";
    public static final String DEACTIVATE_CARD = "/deactivate-card";

    public static final String ADMIN = "/admin";
    public static final String DASHBOARD = "/dashboard";
    public static final String CREATE_CLIENT = "/create-client";

}
