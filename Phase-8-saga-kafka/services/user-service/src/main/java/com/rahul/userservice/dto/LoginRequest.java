package com.rahul.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "must not be blank")
    private String email;

    @NotBlank(message = "must not be blank")
    private String password;
}