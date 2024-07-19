package com.app.MailService.Service;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Repository.OtpRepository;
import com.app.MailService.Utilities.AESHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final List<String> otpTypes = List.of("email", "sms");
    private final List<String> sendTypes = List.of("email", "sms", "card");
    private final List<String> otpStatuses = List.of("pending", "expired", "validated", "rejected");
    private final OtpRepository otpRepository;
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
    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public String createOtp(GenerateOtpRequest request) {

        try {
            String content = AESHelper.decrypt(request.getContent(), aesKey, aesIv);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> data = objectMapper.readValue(content, new TypeReference<>() {
            });

            Otp otp = new Otp(expirationTimeInSecond, maxRetry, maxResend);
            otp.setClientId(data.get("clientId"));
            otp.setType(data.get("type"));
            Map<String, String> sendType = objectMapper.readValue(data.get("sendInfo"), new TypeReference<>() {

            });

            return otp.getTrackingId();
        } catch (Exception e) {
            logger.error("Error while generating OTP: {}", e.getMessage());
            throw new RuntimeException("Error while generating OTP");
        }
    }

}
