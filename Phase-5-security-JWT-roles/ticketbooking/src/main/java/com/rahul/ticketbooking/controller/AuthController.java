package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.RegisterRequest;
import com.rahul.ticketbooking.dto.UserResponse;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.mapper.UserMapper;
import com.rahul.ticketbooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(user));
    }
}