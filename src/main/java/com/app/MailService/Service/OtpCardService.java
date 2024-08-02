package com.app.MailService.Service;

import com.app.MailService.Entity.EmailTemplate;
import com.app.MailService.Entity.OtpCard;
import com.app.MailService.Entity.OtpCardToken;
import com.app.MailService.Model.DTO.SendMailDTO;
import com.app.MailService.Model.DTO.UserGenerateCardInfo;
import com.app.MailService.Model.Projection.OtpCardProjection;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
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
        if (quantity > 20) quantity = 20;
        int generated = 0;
        for (int i = 0; i < quantity; i++) {
            createSingleOtpCard(true);
            generated++;
        }
        log.info("Generated {} OTP cards", generated);
        return generated;
    }

    public boolean userCreateOtpCard(UserGenerateCardInfo userGenerateCardInfo) {
        if (otpCardRepository.existsByUserIdAndStatus(userGenerateCardInfo.getUserId(), Constants.OTP_CARD_STATUS_ACTIVE)) {
            log.error("User {} already has an active OTP card", userGenerateCardInfo.getUserId());
            throw new RuntimeException("User already has an active OTP card");
        }
        OtpCard otpCard = createSingleOtpCard(false);
        otpCard = bindCardToUser(userGenerateCardInfo, otpCard);
        String html = generateOtpCardFileHtmlText(userGenerateCardInfo, otpCard);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.createPdfFromHtml(html, userGenerateCardInfo.getUserProvidedPassword());
        } catch (Exception e) {
            log.error("Error while generating PDF: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        handleUploadToS3AndSendEmail(userGenerateCardInfo, pdfBytes);
        return true;
    }

    @Async
    protected void handleUploadToS3AndSendEmail(UserGenerateCardInfo userGenerateCardInfo, byte[] pdfBytes) {
        String fileName = userGenerateCardInfo.getUserName() + "_" + UUID.randomUUID() + ".pdf";
        String url = s3Service.uploadFile(fileName, pdfBytes);
        sendOtpCardByEmail(userGenerateCardInfo.getUserEmail(), userGenerateCardInfo.getUserName(), url);
    }

    private String generateOtpCardFileHtmlText(UserGenerateCardInfo userGenerateCardInfo, OtpCard otpCard) {
        log.info("Generating OTP card file: {}", otpCard.getCardSerial());
        Map<String, String> fillData = new HashMap<>();
        fillData.put("userName", userGenerateCardInfo.getUserName());
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

    private OtpCard bindCardToUser(UserGenerateCardInfo userGenerateCardInfo, OtpCard otpCard) {
        log.info("Binding card {} to user: {}", otpCard.getCardSerial(), userGenerateCardInfo.getUserId());
        otpCard.setUserId(userGenerateCardInfo.getUserId());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        otpCard.setIssueAt(now);
        Timestamp endOfTheExpiringDay = Timestamp.valueOf(now.toLocalDateTime().plusDays(expirationTimeInDays).with(LocalTime.MAX));
        otpCard.setExpireAt(endOfTheExpiringDay);
        otpCard.setStatus(Constants.OTP_CARD_STATUS_ACTIVE);
        try {
            otpCard.setHashToken(generateOtpCardHash(otpCard));
        } catch (Exception e) {
            log.error("Failed to generate hash token for otp card: {}", otpCard);
            throw new RuntimeException("Failed to generate hash token for otp card");
        }
        return otpCardRepository.save(otpCard);
    }

    private OtpCard createSingleOtpCard(boolean isAdminCreated) {
        String serial = generateSerial(isAdminCreated);
        OtpCard otpCard = new OtpCard();
        otpCard.setCardSerial(serial);
        otpCard.setPublishedAt(new Timestamp(System.currentTimeMillis()));
        otpCard.setStatus(Constants.OTP_CARD_STATUS_AVAILABLE);

        List<OtpCardToken> otpCardTokens = generateCardTokens(otpCard);

        otpCard.setOtpCardTokens(otpCardTokens);
        return otpCardRepository.save(otpCard);
    }

    private String generateSerial(boolean isAdminCreated) {
        String timestampStr = String.valueOf(Instant.now().toEpochMilli());
        String timestampPart = timestampStr.substring(timestampStr.length() - 8);
        return (isAdminCreated ? otpCardPrefixAdmin : otpCardPrefixUser)
                + timestampPart + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private List<OtpCardToken> generateCardTokens(OtpCard otpCard) {
        log.info("Generating card tokens for card serial: {}", otpCard.getCardSerial());
        List<OtpCardToken> result = new ArrayList<>();
        Set<Integer> generatedToken = new HashSet<>();
        int token;
        for (int i = 1; i <= 35; i++) {
            do {
                token = ThreadLocalRandom.current().nextInt(100000, 999999);
            }
            while (generatedToken.contains(token));
            generatedToken.add(token);

            OtpCardToken otpCardToken = new OtpCardToken(otpCard.getCardSerial(), i, String.valueOf(token));
            otpCardToken.setOtpCard(otpCard);
            try {
                otpCardToken.setHashToken(generateOtpCardTokenHash(otpCardToken));
            } catch (Exception e) {
                log.error("Error while generating hash token, skipped serial: {}", otpCardToken.getCardSerial());
                return null;
            }
            result.add(otpCardToken);
        }

        log.info("Finished generating card tokens for serial: {}", otpCard.getCardSerial());
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
        log.info("Sending otp card to email: {}", userEmail);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> content = new HashMap<>();

            try {
                content.put("trackingId", (String) RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
                content.put("clientId", (String) RequestContextHolder.getRequestAttributes().getAttribute("clientId", RequestAttributes.SCOPE_REQUEST));
            } catch (NullPointerException e) {
                content.put("trackingId", UUID.randomUUID().toString());
                content.put("clientId", "admin");
            }

            SendMailDTO sendMailDTO = new SendMailDTO();
            sendMailDTO.setFromAddress(Constants.OTP_CARD_SEND_EMAIL_FROM_ADDRESS);
            sendMailDTO.setSenderName(Constants.OTP_CARD_SEND_EMAIL_SENDER_NAME);
            sendMailDTO.setToAddress(userEmail);
            sendMailDTO.setSubject(Constants.OTP_CARD_EMAIL_SUBJECT);
            sendMailDTO.setEmailTemplate(Constants.OTP_CARD_EMAIL_TEMPLATE);

            Map<String, String> data = new HashMap<>();
            data.put("userName", userName);
            data.put("downloadLink", pdfUrl);

            sendMailDTO.setData(data);

            String strContent = objectMapper.writeValueAsString(sendMailDTO);
            EmailMessageRequest request = new EmailMessageRequest(Constants.OTP_CARD_QUEUE_ROUTING_KEY, strContent, false);
            this.sendMailService.enQueue(request);
        } catch (Exception e) {
            log.error("Error while sending OTP: {}", e.getMessage());
        }
    }

    public OtpCard bindCard(Long userId, String cardSerial) {

        if (otpCardRepository.existsByUserIdAndStatus(userId, Constants.OTP_CARD_STATUS_ACTIVE)) {
            throw new RuntimeException("User already has an active card");
        }

        OtpCard otpCard = otpCardRepository.findByCardSerial(cardSerial, Constants.OTP_CARD_STATUS_AVAILABLE)
                .orElseThrow(() -> new RuntimeException("Card not found or already bound to another user"));

        otpCard.setUserId(userId);
        otpCard.setStatus(Constants.OTP_CARD_STATUS_ACTIVE);
        return otpCardRepository.save(otpCard);
    }

    public Page<OtpCardProjection> fetchOtpCards(Pageable pageRequest, String status) {
        if (status == null) return otpCardRepository.fetchAll(pageRequest);
        else return otpCardRepository.findAllByStatus(status, pageRequest);
    }

    public Page<OtpCardProjection> findUserOtpCards(Pageable pageable, Long userId, String status) {
        if (status == null) return otpCardRepository.findAllByUserId(userId, pageable);
        else return otpCardRepository.findAllByUserIdAndStatus(userId, status, pageable);
    }

    public boolean changeCardStatus(Long id, String status) {
        OtpCard otpCard = otpCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (otpCard.getStatus().equals(Constants.OTP_CARD_STATUS_DEACTIVATED))
            throw new RuntimeException("Cannot modify deactivated card");
        if (otpCard.getStatus().equals(Constants.OTP_CARD_STATUS_AVAILABLE) && !status.equals(Constants.OTP_CARD_STATUS_DEACTIVATED))
            throw new RuntimeException("Invalid status change request for available card");
        if (status.equals(Constants.OTP_CARD_STATUS_LOCKED) && !Constants.OTP_CARD_STATUS_CHANGEABLE_FROM_LOCKED.contains(otpCard.getStatus()))
            throw new RuntimeException("Invalid status change request for locked card");
        if (status.equals(Constants.OTP_CARD_STATUS_ACTIVE) && !Constants.OTP_CARD_STATUS_CHANGEABLE_FROM_ACTIVE.contains(otpCard.getStatus()))
            throw new RuntimeException("Invalid status change request for active card");

        otpCard.setStatus(status);
        otpCardRepository.save(otpCard);
        return true;
    }
}
