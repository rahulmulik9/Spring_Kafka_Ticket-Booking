package com.rahul.ticketbooking.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 100, message = "must be at most 100 characters")
    private String name;

    @NotBlank(message = "must not be blank")
    @Email(message = "must be a valid email")
    @Size(max = 150, message = "must be at most 150 characters")
    private String email;

    @NotBlank(message = "must not be blank")
    @Size(min = 8, max = 64, message = "must be between 8 and 64 characters")
    private String password;
}