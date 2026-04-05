package com.finance.dashboard.service;

import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dto.MonthlyTrendResponse;
import com.finance.dashboard.dto.TransactionResponse;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    // ── Overall summary ────────────────────────────────────────────────────────

    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome   = transactionRepository.sumByType(Transaction.Type.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(Transaction.Type.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);
        long totalTransactions   = transactionRepository.countActive();

        Map<String, BigDecimal> incomeByCategory  = getCategoryMap(Transaction.Type.INCOME);
        Map<String, BigDecimal> expenseByCategory = getCategoryMap(Transaction.Type.EXPENSE);

        List<TransactionResponse> recent = transactionRepository
                .findRecentTransactions(PageRequest.of(0, 10))
                .stream()
                .map(transactionService::toResponse)
                .toList();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .totalTransactions(totalTransactions)
                .incomeByCategory(incomeByCategory)
                .expenseByCategory(expenseByCategory)
                .recentTransactions(recent)
                .build();
    }

    // ── Monthly trends for a given year ───────────────────────────────────────

    public List<MonthlyTrendResponse> getMonthlyTrends(int year) {
        List<Object[]> rows = transactionRepository.monthlyTrendsByYear(year);

        // Initialise all 12 months with zero values
        Map<Integer, BigDecimal[]> map = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            map.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Object[] row : rows) {
            // EXTRACT returns Double in native queries
            int month             = ((Number) row[1]).intValue();
            String typeStr        = row[2].toString().toUpperCase();
            BigDecimal amount     = new BigDecimal(row[3].toString());

            BigDecimal[] pair = map.get(month);
            if (typeStr.equals("INCOME")) {
                pair[0] = amount;
            } else {
                pair[1] = amount;
            }
        }

        List<MonthlyTrendResponse> trends = new ArrayList<>();
        map.forEach((month, pair) -> {
            BigDecimal income   = pair[0];
            BigDecimal expenses = pair[1];
            trends.add(MonthlyTrendResponse.builder()
                    .year(year)
                    .month(month)
                    .monthName(Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .income(income)
                    .expenses(expenses)
                    .net(income.subtract(expenses))
                    .build());
        });

        return trends;
    }

    // ── Category breakdown ─────────────────────────────────────────────────────

    public Map<String, BigDecimal> getCategoryBreakdown(Transaction.Type type) {
        return getCategoryMap(type);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, BigDecimal> getCategoryMap(Transaction.Type type) {
        List<Object[]> rows = transactionRepository.categoryTotalsByType(type);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }
}
