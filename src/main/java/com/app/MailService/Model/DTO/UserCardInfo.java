package com.app.MailService.Model.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Data
public class UserCardInfo {
    private Long userId;
    private String userName;
    private String userEmail;
    private String userProvidedPassword;
}
