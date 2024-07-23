package com.app.MailService.Controller;

import com.app.MailService.Entity.Client;
import com.app.MailService.Repository.ClientRepository;
import com.app.MailService.Utilities.AESHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;

import static com.app.MailService.Utilities.EndPointsConstants.*;

;

@Controller
@RequestMapping(ADMIN)
public class AdminController {
    @Autowired
    private ClientRepository clientRepository;
    @Value("${hashing.key}")
    private String hashingKey;
    @Value("${hashing.iv}")
    private String hashingIv;
    @Value("${aes.key}")
    private String aesKey;
    @Value("${aes.iv}")
    private String aesIv;

    @RequestMapping(DASHBOARD)
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping(CREATE_CLIENT)
    public ResponseEntity<?> createClient(@RequestParam String clientId, @RequestParam String clientSecret) throws Exception {
        if (clientId == null || clientSecret == null) {
            return new ResponseEntity<>("ClientId and ClientSecret are required", HttpStatus.BAD_REQUEST);
        }

        if (clientRepository.existsByClientId(clientId)) {
            return new ResponseEntity<>("Client already exists", HttpStatus.BAD_REQUEST);
        }

        Client client = new Client();
        client.setClientId(clientId);
        client.setClientSecret(AESHelper.encrypt(clientSecret, aesKey, aesIv));
        client.setStatus(Client.STATUS_ACTIVE);
        client.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        String rawText = client.getClientId() + client.getClientSecret() + client.getCreatedAt().toString();
        String hashToken = AESHelper.encrypt(rawText, hashingKey, hashingIv);
        client.setHashToken(hashToken);

        Client savedClient = clientRepository.save(client);
        return new ResponseEntity<>(savedClient, HttpStatus.OK);
    }

    @PostMapping("get-encrypted-content")
    public ResponseEntity<?> getEncryptedContent(@RequestBody Map<String, String> content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String strContent = objectMapper.writeValueAsString(content);
            return new ResponseEntity<>(AESHelper.encrypt(strContent, aesKey, aesIv), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
