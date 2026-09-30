package com.rahul.ticketbooking.config;

import com.rahul.ticketbooking.entity.Role;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfMissing("Admin", "admin@ticket.com", "Admin@123", Role.ADMIN);
        createIfMissing("Organizer", "organizer@ticket.com", "Organizer@123", Role.ORGANIZER);
    }

    private void createIfMissing(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Seeded dev user: {} ({})", email, role);
    }
}