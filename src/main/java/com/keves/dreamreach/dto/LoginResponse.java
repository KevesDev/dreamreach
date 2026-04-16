package com.keves.dreamreach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Returns the generated JWT back to the client upon successful authentication.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type; // We will use this to specify "Bearer" token type
}