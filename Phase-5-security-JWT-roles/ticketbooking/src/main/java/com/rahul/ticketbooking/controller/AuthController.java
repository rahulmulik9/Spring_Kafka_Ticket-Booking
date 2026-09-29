package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.LoginRequest;
import com.rahul.ticketbooking.dto.LoginResponse;
import com.rahul.ticketbooking.dto.RegisterRequest;
import com.rahul.ticketbooking.dto.UserResponse;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.mapper.UserMapper;
import com.rahul.ticketbooking.service.AuthService;
import com.rahul.ticketbooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rahul.ticketbooking.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUser> me(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(user);
    }
}