package com.keves.dreamreach.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the email verification request.
 * Makes sure the frontend passes only the 6-digit code we expect.
 */
@Getter
@Setter
public class VerificationRequest {
    private String code;
}
