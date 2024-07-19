package com.app.MailService.Service;

import com.app.MailService.Utilities.Constants;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
public class GmailService {
    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String APPLICATION_NAME = "MailService";

    @Autowired
    private JavaMailSender mailSender;
    @Value("${application.google.credentials.json}")
    private String credentialsJson;

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new StringReader(credentialsJson));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    private Gmail getGmailService() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private MimeMessage createEmail(String toEmailAddress,
                                    String subject,
                                    String bodyText)
            throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        MimeMessageHelper helper = new MimeMessageHelper(email, true, "UTF-8");

        helper.setFrom(new InternetAddress(Constants.OTP_SEND_EMAIL_FROM_ADDRESS, Constants.OTP_SEND_EMAIL_SENDER_NAME, "UTF-8"));
        helper.setTo(toEmailAddress);
        helper.setSubject(subject);
        helper.setText(bodyText, true);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public boolean sendMailByGmailApi(String toAddress, String subject, String htmlBody) {
        try {
            Gmail gmail = getGmailService();
            MimeMessage email = createEmail(toAddress, subject, htmlBody);
            Message message = createMessageWithEmail(email);
            message = gmail.users().messages().send("me", message).execute();
            logger.info("Message id: {}", message.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error while sending email with Gmail Api: {}", e.getMessage());
            return false;
        }
    }

    //simply use javaMailSender
    public boolean sendSingleMailByJavaMailer(String toAddress, String subject, String htmlBody) {
        try {
            MimeMailMessage message = new MimeMailMessage(mailSender.createMimeMessage());
            MimeMessageHelper helper = new MimeMessageHelper(message.getMimeMessage(), true);

            helper.setFrom(Constants.OTP_SEND_EMAIL_FROM_ADDRESS, Constants.OTP_SEND_EMAIL_SENDER_NAME);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message.getMimeMessage());
            return true;
        } catch (Exception e) {
            logger.error("Error while sending email wih Java Mailer: {}", e.getMessage());
            return false;
        }
    }

}
