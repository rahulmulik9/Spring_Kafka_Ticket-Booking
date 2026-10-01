package com.rahul.ticketbooking.auth.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

// The "who is calling" object. Built from the token, with no database call.
@Getter
@AllArgsConstructor
public class AuthUser {

    private Long id;
    private String email;
    private String role;
}