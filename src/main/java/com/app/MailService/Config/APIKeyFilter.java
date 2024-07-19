package com.app.MailService.Config;

import com.app.MailService.Entity.Client;
import com.app.MailService.Model.DTO.RequestContext;
import com.app.MailService.Repository.ClientRepository;
import com.app.MailService.Utilities.AESHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class APIKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(APIKeyFilter.class);

    @Autowired
    private ClientRepository clientRepository;
    @Value("${aes.key}")
    private String aesKey;
    @Value("${aes.iv}")
    private String aesIv;
    @Value("${hashing.key}")
    private String hashingKey;
    @Value("${hashing.iv}")
    private String hashingIv;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientId = request.getHeader("clientId");
        String clientSecret = request.getHeader("clientSecret");

        if (clientId == null || clientId.isEmpty()) {
            logger.info("Incoming request rejected: clientId header is missing");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "clientId header is missing");
            return;
        }

        Optional<Client> clientWrapper = clientRepository.findByClientId(clientId);
        if (clientWrapper.isEmpty()) {
            logger.info("Incoming request rejected: invalid clientId");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid clientId");
            return;
        }

        Client client = clientWrapper.get();

        if (!validate(client)) {
            logger.info("Incoming request rejected: client validation failed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Client validation failed");
            return;
        }

        if (!client.isActive()) {
            logger.info("Incoming request rejected: client is inactive");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "client is inactive");
            return;
        }

        try {
            if (!AESHelper.encrypt(clientSecret, aesKey, aesIv).equals(client.getClientSecret())) {
                logger.info("Incoming request rejected: invalid clientSecret");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid clientSecret");
                return;
            }
        } catch (Exception e) {
            logger.error("Error while decrypting clientSecret: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while decrypting clientSecret");
            return;
        }

        RequestContext.set("trackingId", UUID.randomUUID().toString());
        RequestContext.set("clientId", clientId);
        logger.info("Incoming request approved, context: {}", RequestContext.getCurrentContext());

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }

    }

    private boolean validate(Client client) {
        String rawText = client.getClientId() + client.getClientSecret() + client.getCreatedAt().toString();
        try {
            return AESHelper.encrypt(rawText, hashingKey, hashingIv).equals(client.getHashToken());
        } catch (Exception e) {
            logger.error("Error while hashing client {}: {}", client.getClientId(), e.getMessage());
            return false;
        }
    }
}
