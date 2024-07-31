package com.app.MailService.Controller;

import com.app.MailService.Entity.Client;
import com.app.MailService.Entity.Projection.OtpCardProjection;
import com.app.MailService.Model.Response.ApiResponse;
import com.app.MailService.Repository.ClientRepository;
import com.app.MailService.Service.OtpCardService;
import com.app.MailService.Utilities.AESHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    @Autowired
    private OtpCardService otpCardService;

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

    @PostMapping(GENERATE_CARDS)
    public ResponseEntity<?> generateCards(@RequestParam(required = false) Integer quantity) {
        if (quantity == null) quantity = 5;
        int cardGenerated = otpCardService.publishOtpCards(quantity);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(HttpStatus.OK.value());
        apiResponse.setMessage("Generated " + cardGenerated + " cards");
        apiResponse.setTimestamp(String.valueOf(new Timestamp(System.currentTimeMillis())));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping(BIND_CARD)
    public ResponseEntity<?> bindCard(@RequestParam Long userId, @RequestParam String cardSerial) {
        otpCardService.bindCard(userId, cardSerial);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(HttpStatus.OK.value());
        apiResponse.setMessage("Successfully bound card to the user");
        apiResponse.setTimestamp(String.valueOf(new Timestamp(System.currentTimeMillis())));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping(GET_CARDS)
    public ResponseEntity<?> getCards(@RequestParam(required = false) Integer pageIndex,
                                      @RequestParam(required = false) Integer pageSize,
                                      @RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) String status) {
        if (pageIndex == null || pageIndex <= 0) pageIndex = 0;
        else pageIndex--;
        if (pageSize == null) pageSize = 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<OtpCardProjection> result = null;

        if (userId == null) {
            result = otpCardService.fetchOtpCards(pageable, status);
        } else {
            result = otpCardService.findUserOtpCards(pageable, userId, status);
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(HttpStatus.OK.value());
        apiResponse.setMessage("Cards fetched successfully");
        apiResponse.setTimestamp(String.valueOf(new Timestamp(System.currentTimeMillis())));
        apiResponse.setData(result);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
