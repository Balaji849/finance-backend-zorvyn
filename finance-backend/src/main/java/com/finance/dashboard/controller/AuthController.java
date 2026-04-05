package com.finance.dashboard.controller;

import com.finance.dashboard.dto.ApiResponse;
import com.finance.dashboard.dto.AuthResponse;
import com.finance.dashboard.dto.LoginRequest;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Public endpoint. Creates a new VIEWER account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    /**
     * POST /api/auth/login
     * Public endpoint. Returns a JWT on success.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}
