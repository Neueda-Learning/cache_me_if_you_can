package com.neueda.transaction_monitor.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.neueda.transaction_monitor.model.AppUser;
import com.neueda.transaction_monitor.repository.AppUserRepository;
import com.neueda.transaction_monitor.security.JwtTokenService;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final JwtTokenService   jwtTokenService;
    private final PasswordEncoder   passwordEncoder;

    public AuthService(AppUserRepository userRepository,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String role, String name) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    // ── Login ─────────────────────────────────────────────────────────────────
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenService.generateToken(
            user.getUsername(), user.getRole(), user.getFullName());

        return new LoginResponse(token, user.getRole(), user.getFullName() != null ? user.getFullName() : user.getUsername());
    }

    // ── Change password (requires authenticated user from SecurityContext) ─────
    public void changePassword(ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        userRepository.updatePassword(username, passwordEncoder.encode(request.newPassword()));
    }

    // ── Admin seeder (called on startup) ──────────────────────────────────────
    public void seedAdminIfAbsent(String username, String plainPassword, String fullName) {
        if (userRepository.countAdmins() == 0) {
            userRepository.save(username,
                passwordEncoder.encode(plainPassword), fullName, null, "ADMIN");
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  HAWK — Default admin account created            ║");
            System.out.println("║  Username : " + username + "                              ║");
            System.out.println("║  Password : " + plainPassword + "                           ║");
            System.out.println("║  Change password after first login!              ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
        }
    }
}

