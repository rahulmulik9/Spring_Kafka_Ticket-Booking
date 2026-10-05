package com.rahul.userservice.service;


import com.rahul.userservice.dto.RegisterRequest;
import com.rahul.userservice.entity.Role;
import com.rahul.userservice.entity.User;
import com.rahul.userservice.exception.EmailAlreadyExistsException;
import com.rahul.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Fast, friendly check for the normal case.
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered: " + email);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());

        try {
            // saveAndFlush sends the INSERT now, so a duplicate fails inside this try block.
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two requests passed the check above at the same time. The UNIQUE constraint caught it.
            throw new EmailAlreadyExistsException("Email already registered: " + email);
        }
    }
}