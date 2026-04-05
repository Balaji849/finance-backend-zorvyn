package com.finance.dashboard.service;

import com.finance.dashboard.config.JwtUtil;
import com.finance.dashboard.dto.AuthResponse;
import com.finance.dashboard.dto.LoginRequest;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.exception.BadRequestException;
import com.finance.dashboard.exception.ConflictException;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user. Only ADMINs can assign roles other than VIEWER via
     * the /users endpoint; public registration always creates a VIEWER.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        log.info("New user registered: {} [{}]", saved.getEmail(), saved.getRole());

        String token = jwtUtil.generate(saved.getEmail(), saved.getRole().name());
        return buildResponse(saved, token);
    }

    /**
     * Authenticate a user and return a JWT.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadRequestException("Your account has been deactivated. Contact an administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtil.generate(user.getEmail(), user.getRole().name());
        log.info("User logged in: {} [{}]", user.getEmail(), user.getRole());
        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
