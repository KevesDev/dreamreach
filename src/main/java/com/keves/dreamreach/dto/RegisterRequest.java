package com.keves.dreamreach.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * The DTO for the registration request. Ensures that the only
 * thing the user can pass to the system is the data we ask for,
 * preventing injection hacks.
 */
@Getter
@Setter
public class RegisterRequest {
    private String password;
    private String email;
}
