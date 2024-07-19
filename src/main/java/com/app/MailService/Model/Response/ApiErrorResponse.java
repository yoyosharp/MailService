package com.app.MailService.Model.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private String timestamp;
    private Integer status;
    private String error;
    private String path;
}
