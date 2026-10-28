package com.ga.waslah.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;

    // Role at login time, e.g. USER or CAFE_MANAGER (a UI hint; the API authorizes from the database).
    private String role;
}