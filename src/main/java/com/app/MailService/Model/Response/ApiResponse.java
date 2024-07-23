package com.app.MailService.Model.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse {
    private String timestamp;
    private Integer status;
    private String message;
    private Object result;
}
