package com.app.MailService.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse {
    private String timestamp;
    private String status;
    private String trackingId;
    private String url;
}
