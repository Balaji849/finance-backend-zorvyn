package com.finance.dashboard.controller;

import com.finance.dashboard.dto.ApiResponse;
import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dto.MonthlyTrendResponse;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/summary
     * Returns total income, expenses, net balance, category breakdown,
     * and the 10 most recent transactions.
     * ANALYST and ADMIN only.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSummary()));
    }

    /**
     * GET /api/dashboard/trends?year=2024
     * Returns income vs expense totals broken down by month for the given year.
     * Defaults to the current year.
     * ANALYST and ADMIN only.
     */
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> trends(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {

        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMonthlyTrends(year)));
    }

    /**
     * GET /api/dashboard/categories?type=EXPENSE
     * Returns spending or income totals grouped by category.
     * ANALYST and ADMIN only.
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> categoryBreakdown(
            @RequestParam(defaultValue = "EXPENSE") String type) {

        Transaction.Type typeEnum;
        try {
            typeEnum = Transaction.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid type. Must be INCOME or EXPENSE"));
        }

        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryBreakdown(typeEnum)));
    }
}
