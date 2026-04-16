package com.keves.dreamreach.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Ensures the frontend only passes the exact credentials we expect for login.
 */
@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}