package com.finance.dashboard.controller;

import com.finance.dashboard.dto.ApiResponse;
import com.finance.dashboard.dto.TransactionRequest;
import com.finance.dashboard.dto.TransactionResponse;
import com.finance.dashboard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * GET /api/transactions
     * All authenticated users can view transactions.
     * Supports filtering by category, type, date range and pagination.
     *
     * Query params:
     *   category  — filter by category name (case-insensitive)
     *   type      — INCOME or EXPENSE
     *   from      — start date (yyyy-MM-dd)
     *   to        — end date   (yyyy-MM-dd)
     *   page      — page number (default 0)
     *   size      — page size  (default 20, max 100)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap page size to prevent abuse
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<TransactionResponse> result = transactionService.list(category, type, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * GET /api/transactions/{id}
     * All authenticated users can view a single transaction.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getById(id)));
    }

    /**
     * POST /api/transactions
     * ANALYST and ADMIN can create transactions.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest request,
            Authentication auth) {

        // Pass the authenticated user's email so the service can link the record
        TransactionResponse created = transactionService.create(request, auth.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction created", created));
    }

    /**
     * PUT /api/transactions/{id}
     * ADMIN only can update transactions.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse updated = transactionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Transaction updated", updated));
    }

    /**
     * DELETE /api/transactions/{id}
     * ADMIN only — soft delete (sets deleted = true).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction deleted successfully"));
    }
}
