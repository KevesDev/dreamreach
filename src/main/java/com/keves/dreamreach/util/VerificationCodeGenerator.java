package com.keves.dreamreach.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

/**
 * Utility component for generating cryptographically secure 6-digit codes.
 */
@Component
public class VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random 6-digit number between 100000 and 999999.
     */
    public String generateCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}