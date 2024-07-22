package com.app.MailService.Config;

import com.app.MailService.Entity.Client;
import com.app.MailService.Repository.ClientRepository;
import com.app.MailService.Utilities.AESHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class APIKeyFilter extends OncePerRequestFilter {


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
            log.info("Incoming request rejected: clientId header is missing");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "clientId header is missing");
            return;
        }

        Optional<Client> clientWrapper = clientRepository.findByClientId(clientId);
        if (clientWrapper.isEmpty()) {
            log.info("Incoming request rejected: invalid clientId");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid clientId");
            return;
        }

        Client client = clientWrapper.get();

        if (!validate(client)) {
            log.info("Incoming request rejected: client validation failed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Client validation failed");
            return;
        }

        if (!client.isActive()) {
            log.info("Incoming request rejected: client is inactive");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "client is inactive");
            return;
        }

        try {
            if (!AESHelper.encrypt(clientSecret, aesKey, aesIv).equals(client.getClientSecret())) {
                log.info("Incoming request rejected: invalid clientSecret");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid clientSecret");
                return;
            }
        } catch (Exception e) {
            log.error("Error while decrypting clientSecret: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while decrypting clientSecret");
            return;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            log.info("Incoming request rejected: requestAttributes is null");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Something went wrong");
            return;
        }
        requestAttributes.setAttribute("trackingId", UUID.randomUUID().toString(), RequestAttributes.SCOPE_REQUEST);
        requestAttributes.setAttribute("clientId", clientId, RequestAttributes.SCOPE_REQUEST);
        log.info("Incoming request approved, trackingId: {}, clientId: {}",
                requestAttributes.getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST),
                requestAttributes.getAttribute("clientId", RequestAttributes.SCOPE_REQUEST));

        filterChain.doFilter(request, response);
    }

    private boolean validate(Client client) {
        String rawText = client.getClientId() + client.getClientSecret() + client.getCreatedAt().toString();
        try {
            return AESHelper.encrypt(rawText, hashingKey, hashingIv).equals(client.getHashToken());
        } catch (Exception e) {
            log.error("Error while hashing client {}: {}", client.getClientId(), e.getMessage());
            return false;
        }
    }
}
