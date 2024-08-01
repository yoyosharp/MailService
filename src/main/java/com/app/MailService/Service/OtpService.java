package com.app.MailService.Service;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Entity.OtpCard;
import com.app.MailService.Exception.OtpException;
import com.app.MailService.Model.DTO.GenerateOtpDTO;
import com.app.MailService.Model.DTO.VerifyOtpDTO;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Model.Request.VerifyOtpRequest;
import com.app.MailService.Repository.OtpCardRepository;
import com.app.MailService.Utilities.AESHelper;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class OtpService {

    private final OtpCacheService otpCacheService;
    private final SendMailService sendMailService;
    private final OtpCardRepository otpCardRepository;
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
    public OtpService(OtpCacheService otpCacheService, SendMailService sendMailService, OtpCardRepository otpCardRepository) {
        this.otpCacheService = otpCacheService;
        this.sendMailService = sendMailService;
        this.otpCardRepository = otpCardRepository;
    }

    @Transactional
    public Otp createOtp(GenerateOtpRequest request) {

        try {
            String content = AESHelper.decrypt(request.getContent(), aesKey, aesIv);
            ObjectMapper objectMapper = new ObjectMapper();
            GenerateOtpDTO data = objectMapper.readValue(content, new TypeReference<>() {
            });
            Otp otp = generateOtp(data);

            Map<String, String> sendInfo = objectMapper.readValue(data.getSendInfo(), new TypeReference<>() {
            });
            String sendType = sendInfo.get("sendType");
            log.info("Generating otp for request: {}, sendType: {}",
                    RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST),
                    sendType);

            if (Constants.OTP_SEND_TYPE_CARD.equals(sendType)) {
                Map<String, String> otpByCardInfo = proceedOtpByCard(sendInfo);
                otp.setOtpCode(otpByCardInfo.get("otpCode"));
                sendInfo.put("position", otpByCardInfo.get("position"));
                otp.setSendInfo(objectMapper.writeValueAsString(sendInfo));
            } else if (Constants.OTP_SEND_TYPE_EMAIL.equals(sendType)) {
                otp.setOtpCode(String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999)));
                sendOtpByEmail(otp, sendInfo);
            } else if (Constants.OTP_SEND_TYPE_SMS.equals(sendType)) {
                otp.setOtpCode(String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999)));
                sendOtpBySms(otp, sendInfo);
            } else {
                log.info("Invalid OTP send type, requested send type: {}", sendType);
                throw new RuntimeException("Invalid OTP send type");
            }

            otpCacheService.saveOtp(otp);
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
        Map<String, String> otpByCardData = new HashMap<>(sendInfo);
        Long userId = Long.parseLong(sendInfo.get("target"));
        OtpCard userOtpCard = otpCardRepository.getUserOtpCard(userId, Constants.OTP_CARD_STATUS_ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active otp card found for user: " + userId));

        Integer position = ThreadLocalRandom.current().nextInt(1, 36);

        String token = userOtpCard.getOtpCardTokens().stream()
                .filter(t -> Objects.equals(t.getPosition(), position))
                .findFirst()
                .get()
                .getToken();
        otpByCardData.put("otpCode", token);
        otpByCardData.put("position", String.valueOf(position));
        return otpByCardData;
    }

    @Async
    protected void sendOtpBySms(Otp otp, Map<String, String> sendInfo) {

    }

    @Async
    protected void sendOtpByEmail(Otp otp, Map<String, String> sendInfo) {
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

    @Transactional
    public Otp verifyOtp(VerifyOtpRequest request) {
        log.info("Trying to verify the OTP, request Id: {}",
                RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
        try {
            String strRequest = AESHelper.decrypt(request.getContent(), aesKey, aesIv);
            ObjectMapper objectMapper = new ObjectMapper();

            VerifyOtpDTO data = objectMapper.readValue(strRequest, new TypeReference<>() {
            });
            Otp otp = otpCacheService.getOtpByTrackingId(data.getTrackingId());
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

            otpCacheService.updateOtp(otp);
            return otp;
        } catch (OtpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while verifying OTP: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Otp resendOtp(String trackingId) {
        log.info("Trying to resend the OTP, request Id: {}, trackingId: {}",
                RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST),
                trackingId);
        Otp otp = otpCacheService.getOtpByTrackingId(trackingId);
        if (otp == null) {
            log.info("Could not find OTP: {}", trackingId);
            throw new RuntimeException("Could not find OTP");
        }

        statusCheck(otp);
        retryingCheck(otp);
        expiringCheck(otp);
        resendingCheck(otp);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, String> sendInfo = objectMapper.readValue(otp.getSendInfo(), new TypeReference<>() {
            });
            String sendType = sendInfo.get("sendType");
            if (Constants.OTP_SEND_TYPE_EMAIL.equals(sendType)) {
                sendOtpByEmail(otp, sendInfo);
            } else if (Constants.OTP_SEND_TYPE_SMS.equals(sendType)) {
                sendOtpBySms(otp, sendInfo);
            } else {
                log.info("Error while resending OTP: Invalid OTP send type, requested send type: {}", sendType);
                throw new RuntimeException("Error while resending OTP");
            }
        } catch (JsonProcessingException e) {
            log.error("Error while parsing sendInfo: {}", e.getMessage());
            throw new RuntimeException("Error while resending OTP");
        }
        otp.setResendCount(otp.getResendCount() + 1);
        otpCacheService.updateOtp(otp);
        return otp;
    }

    private void integrityCheck(Otp otp) {
        String strData = otp.getTrackingId() + otp.getType() + otp.getCreatedAt() + otp.getExpiringTime();
        try {
            String hashCode = AESHelper.encrypt(strData, aesKey, aesIv);
            if (!hashCode.equals(otp.getHashToken())) {
                otp.setStatus(Constants.OTP_STATUS_REJECTED);
                otpCacheService.updateOtp(otp);
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
            otpCacheService.updateOtp(otp);
            log.info("OTP expired: {}", otp.getTrackingId());
            throw new OtpException("OTP expired");
        }
    }

    private void retryingCheck(Otp otp) {
        if (otp.getRetryCount() >= otp.getMaxRetry()) {
            otp.setStatus(Constants.OTP_STATUS_REJECTED);
            otpCacheService.updateOtp(otp);
            log.info("Max retry count reached: {}", otp.getTrackingId());
            throw new OtpException("Max retry count reached");
        }
    }

    private void resendingCheck(Otp otp) {
        if (otp.getResendCount() >= otp.getMaxResend()) {
            otp.setStatus(Constants.OTP_STATUS_REJECTED);
            otpCacheService.updateOtp(otp);
            log.info("Max resend count reached: {}", otp.getTrackingId());
            throw new OtpException("Max resend count reached");
        }
    }
}
