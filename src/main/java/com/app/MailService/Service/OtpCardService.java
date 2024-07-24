package com.app.MailService.Service;

import com.app.MailService.Entity.EmailTemplate;
import com.app.MailService.Entity.OtpCard;
import com.app.MailService.Entity.OtpCardToken;
import com.app.MailService.Model.DTO.UserCardInfo;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Repository.EmailTemplateRepository;
import com.app.MailService.Repository.OtpCardRepository;
import com.app.MailService.Repository.OtpCardTokenRepository;
import com.app.MailService.Utilities.AESHelper;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class OtpCardService {

    private final OtpCardRepository otpCardRepository;
    private final OtpCardTokenRepository otpCardTokenRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final PdfService pdfService;
    private final S3Service s3Service;
    private final SendMailService sendMailService;
    @Value("${aes.key}")
    private String aesKey;
    @Value("${aes.iv}")
    private String aesIv;
    @Value("${application.otpCard.expirationTimeInDays}")
    private int expirationTimeInDays;
    @Value("${application.otpCard.prefix.admin}")
    private String otpCardPrefixAdmin;
    @Value("${application.otpCard.prefix.user}")
    private String otpCardPrefixUser;

    @Autowired
    public OtpCardService(OtpCardRepository otpCardRepository, OtpCardTokenRepository otpCardTokenRepository, EmailTemplateRepository emailTemplateRepository, PdfService pdfService, S3Service s3Service, SendMailService sendMailService) {
        this.otpCardRepository = otpCardRepository;
        this.otpCardTokenRepository = otpCardTokenRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.pdfService = pdfService;
        this.s3Service = s3Service;
        this.sendMailService = sendMailService;
    }

    public int publishOtpCards(int quantity) {
        List<OtpCard> otpCards = new ArrayList<>();
        int generated = 0;
        for (int i = 0; i < quantity; i++) {
            OtpCard otpCard = createSingleOtpCard(true);
            generated++;
        }
        log.info("Generated {} OTP cards", generated);
        return generated;
    }

    public boolean userCreateOtpCard(UserCardInfo userCardInfo) {
        OtpCard otpCard = createSingleOtpCard(false);

        otpCard = bindCardToUser(userCardInfo, otpCard);

        String html = generateOtpCardFileHtmlText(userCardInfo, otpCard);

        String url = "";
        try {
            byte[] pdfBytes = pdfService.createPdfFromHtml(html, userCardInfo.getUserProvidedPassword());
            String fileName = userCardInfo.getUserName() + "_" + UUID.randomUUID() + ".pdf";
            url = s3Service.uploadFile(fileName, pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        sendOtpCardByEmail(userCardInfo.getUserEmail(), userCardInfo.getUserName(), url);

        return true;
    }

    private String generateOtpCardFileHtmlText(UserCardInfo userCardInfo, OtpCard otpCard) {
        log.info("Generating OTP card file: {}", otpCard.getCardSerial());
        Map<String, String> fillData = new HashMap<>();
        fillData.put("userName", userCardInfo.getUserName());
        fillData.put("cardSerial", otpCard.getCardSerial());
        otpCard.getOtpCardTokens().forEach(otpCardToken -> {
            String pos = "pos" + otpCardToken.getPosition();
            fillData.put(pos, otpCardToken.getToken());
        });

        EmailTemplate pdfTemplate = emailTemplateRepository.findByName(Constants.OTP_CARD_TEMPLATE_NAME);
        if (pdfTemplate == null) {
            log.error("Failed to find email template: {}", Constants.OTP_CARD_TEMPLATE_NAME);
            throw new RuntimeException("Failed to find email template: " + Constants.OTP_CARD_TEMPLATE_NAME);
        }

        return pdfTemplate.fillTemplate(fillData);
    }

    private OtpCard bindCardToUser(UserCardInfo userCardInfo, OtpCard otpCard) {
        log.info("Binding card {} to user: {}", otpCard.getCardSerial(), userCardInfo.getUserId());
        otpCard.setUserId(userCardInfo.getUserId());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        otpCard.setIssueAt(now);
        Timestamp endOfTheExpiringDay = Timestamp.valueOf(now.toLocalDateTime().plusDays(expirationTimeInDays).with(LocalTime.MAX));
        otpCard.setExpireAt(endOfTheExpiringDay);
        try {
            otpCard.setHashToken(generateOtpCardHash(otpCard));
        } catch (Exception e) {
            log.error("Failed to generate hash token for otp card: {}", otpCard);
            throw new RuntimeException("Failed to generate hash token for otp card");
        }
        otpCard.setStatus(Constants.OTP_CARD_STATUS_ACTIVE);

        return otpCardRepository.save(otpCard);
    }

    private OtpCard createSingleOtpCard(boolean isAdminCreated) {
        String serial = generateSerial(isAdminCreated);
        List<OtpCardToken> otpCardTokens = generateCardTokens(serial);

        OtpCard otpCard = new OtpCard();
        otpCard.setCardSerial(serial);
        otpCard.setOtpCardTokens(otpCardTokens);
        otpCard.setPublishedAt(new Timestamp(System.currentTimeMillis()));
        otpCard.setStatus(Constants.OTP_CARD_STATUS_AVAILABLE);

        return otpCardRepository.save(otpCard);
    }

    private String generateSerial(boolean isAdminCreated) {
        String timestampStr = String.valueOf(Instant.now().toEpochMilli());
        String timestampPart = timestampStr.substring(timestampStr.length() - 8);
        return isAdminCreated ? otpCardPrefixAdmin : otpCardPrefixUser
                + timestampPart + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private List<OtpCardToken> generateCardTokens(String serial) {
        log.info("Generating card tokens for serial: {}", serial);
        List<OtpCardToken> result = new ArrayList<>();
        Set<Integer> generatedToken = new HashSet<>();
        int token;
        for (int i = 1; i <= 35; i++) {
            do {
                token = ThreadLocalRandom.current().nextInt(100000, 999999);
            }
            while (generatedToken.contains(token));
            generatedToken.add(token);

            OtpCardToken otpCardToken = new OtpCardToken(serial, i, String.valueOf(token));
            try {
                otpCardToken.setHashToken(generateOtpCardTokenHash(otpCardToken));
            } catch (Exception e) {
                log.error("Error while generating hash token, skipped serial: {}", serial);
                return null;
            }
            result.add(otpCardToken);
        }

        log.info("Finished generating card tokens for serial: {}", serial);
        return result;
    }

    private String generateOtpCardHash(OtpCard otpCard) throws Exception {
        String content = otpCard.getUserId() + otpCard.getCardSerial() + otpCard.getIssueAt() + otpCard.getExpireAt() + otpCard.getStatus();
        return AESHelper.encrypt(content, aesKey, aesIv);
    }

    private String generateOtpCardTokenHash(OtpCardToken otpCardToken) throws Exception {
        String content = otpCardToken.getCardSerial() + otpCardToken.getPosition() + otpCardToken.getToken() + otpCardToken.getCreateAt();
        return AESHelper.encrypt(content, aesKey, aesIv);
    }

    private void sendOtpCardByEmail(String userEmail, String userName, String pdfUrl) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> content = new HashMap<>();

            content.put("trackingId", (String) RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
            content.put("clientId", (String) RequestContextHolder.getRequestAttributes().getAttribute("clientId", RequestAttributes.SCOPE_REQUEST));
            content.put("fromAddress", Constants.OTP_CARD_SEND_EMAIL_FROM_ADDRESS);
            content.put("senderName", Constants.OTP_CARD_SEND_EMAIL_SENDER_NAME);
            content.put("toAddress", userEmail);
            content.put("subject", Constants.OTP_CARD_EMAIL_SUBJECT);
            content.put("emailTemplate", Constants.OTP_CARD_EMAIL_TEMPLATE);

            Map<String, String> data = new HashMap<>();
            data.put("userName", userName);
            data.put("downloadLink", pdfUrl);
            String strData = objectMapper.writeValueAsString(data);
            content.put("data", strData);

            String strContent = objectMapper.writeValueAsString(content);
            EmailMessageRequest request = new EmailMessageRequest(Constants.OTP_CARD_QUEUE_ROUTING_KEY, strContent, false);
            this.sendMailService.enQueue(request);
        } catch (Exception e) {
            log.error("Error while sending OTP: {}", e.getMessage());
        }
    }
}