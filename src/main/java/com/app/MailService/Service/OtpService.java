package com.app.MailService.Service;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Exception.OtpException;
import com.app.MailService.Model.DTO.GenerateOtpDTO;
import com.app.MailService.Model.DTO.VerifyOtpDTO;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Model.Request.VerifyOtpRequest;
import com.app.MailService.Repository.OtpRepository;
import com.app.MailService.Utilities.AESHelper;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class OtpService {

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
            GenerateOtpDTO data = objectMapper.readValue(content, new TypeReference<>() {
            });
            Map<String, String> sendInfo = objectMapper.readValue(data.getSendInfo(), new TypeReference<>() {
            });

            Otp otp = generateOtp(data);
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
                    log.info("Invalid OTP send type, requested send type: {}", sendType);
                    throw new RuntimeException("Invalid OTP send type");
                }
            }

            otpRepository.save(otp);
            return otp;
        } catch (Exception e) {
            log.error("Error while generating OTP: {}", e.getMessage());
            throw new RuntimeException("Error while generating OTP");
        }
    }

    private Otp generateOtp(GenerateOtpDTO data) {
        Otp otp = new Otp();
        otp.setTrackingId((String) RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
        otp.setClientId((String) RequestContextHolder.getRequestAttributes().getAttribute("clientId", RequestAttributes.SCOPE_REQUEST));
        otp.setType(data.getOtpType());
        otp.setSendInfo(data.getSendInfo());
        otp.setStatus(Constants.OTP_STATUS_PENDING);
        otp.setMaxRetry(maxRetry);
        otp.setMaxResend(maxResend);
        otp.setRetryCount(0);
        otp.setResendCount(0);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        otp.setCreatedAt(now);
        otp.setExpiringTime(Timestamp.valueOf(now.toLocalDateTime().plusSeconds(expirationTimeInSecond)));

        String strData = otp.getTrackingId() + otp.getType() + otp.getCreatedAt() + otp.getExpiringTime();
        try {
            otp.setHashToken(AESHelper.encrypt(strData, aesKey, aesIv));
        } catch (Exception e) {
            log.error("Error while hashing OTP: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        return otp;

    }

    private Map<String, String> proceedOtpByCard(Map<String, String> sendInfo) {
        //TO DO : implement otp by card
        return null;
    }

    private void sendOtpBySms(Otp otp, Map<String, String> sendInfo) {

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
            EmailMessageRequest request = new EmailMessageRequest(otp.getType(), strContent, false);
            this.sendMailService.enQueue(request);
        } catch (Exception e) {
            log.error("Error while sending OTP: {}", e.getMessage());
        }
    }

    public Otp verifyOtp(VerifyOtpRequest request) {
        log.info("Trying to verify the OTP, request Id: {}",
                RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
        try {
            String strRequest = AESHelper.decrypt(request.getContent(), aesKey, aesIv);
            ObjectMapper objectMapper = new ObjectMapper();

            VerifyOtpDTO data = objectMapper.readValue(strRequest, new TypeReference<>() {
            });
            Otp otp = otpRepository.findByTrackingId(data.getTrackingId());
            if (otp == null) {
                log.info("Could not find OTP: {}", data.getTrackingId());
                throw new RuntimeException("Could not find OTP");
            }
            integrityCheck(otp);
            statusCheck(otp);
            retryingCheck(otp);
            expiringCheck(otp);

            if (data.getOtpCode().equals(otp.getOtpCode())) {
                otp.setStatus(Constants.OTP_STATUS_VERIFIED);
                log.info("Successfully verified OTP: {}", data.getTrackingId());
            } else {
                otp.setRetryCount(otp.getRetryCount() + 1);
                log.info("Failed to verify OTP: {}, input otp code: {}", data.getTrackingId(), data.getOtpCode());
            }
            otpRepository.save(otp);

            return otp;
        } catch (OtpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while verifying OTP: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void integrityCheck(Otp otp) {
        String strData = otp.getTrackingId() + otp.getType() + otp.getCreatedAt() + otp.getExpiringTime();
        try {
            String hashCode = AESHelper.encrypt(strData, aesKey, aesIv);
            if (!hashCode.equals(otp.getHashToken())) {
                otp.setStatus(Constants.OTP_STATUS_REJECTED);
                otpRepository.save(otp);
                throw new Exception("Hash token mismatch");
            }
        } catch (Exception e) {
            log.error("Failed to integrating check the OTP: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void statusCheck(Otp otp) {
        if (!otp.getStatus().equals(Constants.OTP_STATUS_PENDING)) {
            log.info("Invalid OTP status: {}", otp.getTrackingId());
            throw new RuntimeException("Invalid OTP status");
        }
    }

    private void expiringCheck(Otp otp) {
        if (otp.getExpiringTime().before(new Timestamp(System.currentTimeMillis()))) {
            otp.setStatus(Constants.OTP_STATUS_EXPIRED);
            otpRepository.save(otp);
            log.info("OTP expired: {}", otp.getTrackingId());
            throw new OtpException("OTP expired");
        }
    }

    private void retryingCheck(Otp otp) {
        if (otp.getRetryCount() >= otp.getMaxRetry()) {
            otp.setStatus(Constants.OTP_STATUS_REJECTED);
            otpRepository.save(otp);
            log.info("Max retry count reached: {}", otp.getTrackingId());
            throw new OtpException("Max retry count reached");
        }
    }
}
