package com.app.MailService.Service;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Model.DTO.OtpData;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Repository.OtpRepository;
import com.app.MailService.Utilities.AESHelper;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpRepository otpRepository;
    private final SendMailService sendMailService;
    @Value("${aes.key}")
    private String aesKey;
    @Value("${aes.iv}")
    private String aesIv;
    @Value("${application.otp.maxRetry}")
    private int maxRetry;
    @Value("${application.otp.maxResend}")
    private int maxResend;
    @Value("${application.otp.expirationTimeInSecond}")
    private long expirationTimeInSecond;

    @Autowired
    public OtpService(OtpRepository otpRepository, SendMailService sendMailService) {
        this.otpRepository = otpRepository;
        this.sendMailService = sendMailService;
    }

    @Transactional
    public Otp createOtp(GenerateOtpRequest request) {

        try {
            String content = AESHelper.decrypt(request.getContent(), aesKey, aesIv);
            ObjectMapper objectMapper = new ObjectMapper();
            OtpData data = objectMapper.readValue(content, new TypeReference<>() {
            });

            Otp otp = new Otp(data, maxRetry, maxResend);
            otp.setExpiringTime(Timestamp.valueOf(otp.getCreatedAt().toLocalDateTime().plusSeconds(expirationTimeInSecond)));
            Map<String, String> sendInfo = objectMapper.readValue(data.getSendInfo(), new TypeReference<>() {
            });
            String sendType = sendInfo.get("sendType");

            switch (sendType) {
                case Constants.OTP_SEND_TYPE_EMAIL -> {
                    otp.setOtpCode(String.valueOf(new Random().nextInt(100000, 999999)));
                    sendOtpByEmail(otp, sendInfo);
                }
                case Constants.OTP_SEND_TYPE_SMS -> {
                    otp.setOtpCode(String.valueOf(new Random().nextInt(100000, 999999)));
                    sendOtpBySms(otp, sendInfo);
                }
                case Constants.OTP_SEND_TYPE_CARD -> {
                    Map<String, String> otpByCardInfo = proceedOtpByCard(sendInfo);
                    otp.setOtpCode(otpByCardInfo.get("otpCode"));
                    sendInfo.put("target", otpByCardInfo.get("position"));
                    otp.setSendInfo(objectMapper.writeValueAsString(sendInfo));
                }
                default -> {
                    logger.info("Invalid OTP send type, requested send type: {}", sendType);
                    throw new RuntimeException("Invalid OTP send type");
                }
            }

            otpRepository.save(otp);
            return otp;
        } catch (Exception e) {
            logger.error("Error while generating OTP: {}", e.getMessage());
            throw new RuntimeException("Error while generating OTP");
        }
    }

    private Map<String, String> proceedOtpByCard(Map<String, String> sendInfo) {
        return null;
    }

    private void sendOtpBySms(Otp otp, Map<String, String> sendInfo) throws JsonProcessingException {

    }

    private void sendOtpByEmail(Otp otp, Map<String, String> sendInfo) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> content = new HashMap<>();

            content.put("trackingId", otp.getTrackingId());
            content.put("clientId", otp.getClientId());
            content.put("fromAddress", Constants.OTP_SEND_EMAIL_FROM_ADDRESS);
            content.put("senderName", Constants.OTP_SEND_EMAIL_SENDER_NAME);
            content.put("toAddress", sendInfo.get("target"));
            content.put("subject", Constants.OTP_SEND_EMAIL_SUBJECT);
            content.put("emailTemplate", otp.getType());

            Map<String, String> data = new HashMap<>();
            data.put("userName", sendInfo.get("userName"));
            data.put("otpCode", otp.getOtpCode());
            String strData = objectMapper.writeValueAsString(data);
            content.put("data", strData);

            String strContent = objectMapper.writeValueAsString(content);
            EmailMessageRequest request = new EmailMessageRequest("demo", strContent, false);
            this.sendMailService.enQueue(request);
        } catch (Exception e) {
            logger.error("Error while sending OTP: {}", e.getMessage());
        }

    }

}
