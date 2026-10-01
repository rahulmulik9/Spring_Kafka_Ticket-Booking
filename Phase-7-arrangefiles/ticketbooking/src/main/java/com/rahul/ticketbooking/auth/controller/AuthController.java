package com.rahul.ticketbooking.auth.controller;

import com.rahul.ticketbooking.user.entity.User;
import com.rahul.ticketbooking.user.mapper.UserMapper;
import com.rahul.ticketbooking.auth.service.AuthService;
import com.rahul.ticketbooking.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rahul.ticketbooking.auth.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import com.rahul.ticketbooking.auth.dto.LoginRequest;
import com.rahul.ticketbooking.auth.dto.LoginResponse;
import com.rahul.ticketbooking.auth.dto.RefreshRequest;
import com.rahul.ticketbooking.auth.dto.RegisterRequest;
import com.rahul.ticketbooking.user.dto.UserResponse;

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


    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}