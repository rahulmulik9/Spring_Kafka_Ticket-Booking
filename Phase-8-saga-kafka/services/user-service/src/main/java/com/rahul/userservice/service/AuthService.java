package com.rahul.userservice.service;

import com.rahul.userservice.dto.LoginRequest;
import com.rahul.userservice.dto.LoginResponse;
import com.rahul.userservice.dto.RefreshRequest;
import com.rahul.userservice.entity.User;
import com.rahul.userservice.exception.InvalidCredentialsException;
import com.rahul.userservice.repository.UserRepository;
import com.rahul.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Count this attempt first. A blocked request never reaches the database or BCrypt.
        loginRateLimiter.checkAndCount(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        loginRateLimiter.reset(email);   // proved who they are, so start counting from zero again
        return buildTokens(user);
    }

    // One transaction: if issuing the new token fails, the old one is not lost.
    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.consume(request.getRefreshToken());
        return buildTokens(user);
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    private LoginResponse buildTokens(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtService.getExpirationSeconds());
    }
}