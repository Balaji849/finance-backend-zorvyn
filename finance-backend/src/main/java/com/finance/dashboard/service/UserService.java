package com.finance.dashboard.service;

import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.dto.UpdateUserRequest;
import com.finance.dashboard.dto.UserResponse;
import com.finance.dashboard.exception.ConflictException;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── List all users ─────────────────────────────────────────────────────────

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserResponse> getActiveUsers() {
        return userRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Get single user ────────────────────────────────────────────────────────

    public UserResponse getUserById(String id) {
        return toResponse(findById(id));
    }

    // ── Create user (admin only) ───────────────────────────────────────────────

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        User.Role role = (request.getRole() != null) ? request.getRole() : User.Role.VIEWER;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created user: {} [{}]", saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    // ── Update user ────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = findById(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User updated = userRepository.save(user);
        log.info("User updated: {} → role={}, active={}", updated.getEmail(), updated.getRole(), updated.isActive());
        return toResponse(updated);
    }

    // ── Delete user (hard delete, admin only) ──────────────────────────────────

    @Transactional
    public void deleteUser(String id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
