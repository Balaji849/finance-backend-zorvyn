package com.finance.dashboard.controller;

import com.finance.dashboard.dto.ApiResponse;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.dto.UpdateUserRequest;
import com.finance.dashboard.dto.UserResponse;
import com.finance.dashboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * ADMIN only — list all users.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    /**
     * GET /api/users/active
     * ADMIN only — list active users only.
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getActiveUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getActiveUsers()));
    }

    /**
     * GET /api/users/{id}
     * ADMIN only — get a single user by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    /**
     * POST /api/users
     * ADMIN only — create a user with any role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse created = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", created));
    }

    /**
     * PUT /api/users/{id}
     * ADMIN only — update name, role, or active status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }

    /**
     * DELETE /api/users/{id}
     * ADMIN only — permanently delete a user.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully"));
    }
}
