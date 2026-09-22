package com.hms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String message;
    private String token;

    /** Long-lived, and only good for obtaining a new access token. */
    private String refreshToken;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean success;

}