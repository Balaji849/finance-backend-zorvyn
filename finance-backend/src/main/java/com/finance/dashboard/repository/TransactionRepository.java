package com.finance.dashboard.repository;

import com.finance.dashboard.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // ── Filtered listing ──────────────────────────────────────────────────────
    // Use 4 separate queries to avoid null-cast issues with PostgreSQL enums

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findAll(Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND t.type = :type
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByType(@Param("type") Transaction.Type type, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND LOWER(t.category) = LOWER(:category)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND t.type = :type
        AND LOWER(t.category) = LOWER(:category)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByTypeAndCategory(
            @Param("type") Transaction.Type type,
            @Param("category") String category,
            Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND (:from IS NULL OR t.date >= :from)
        AND (:to   IS NULL OR t.date <= :to)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND t.type = :type
        AND (:from IS NULL OR t.date >= :from)
        AND (:to   IS NULL OR t.date <= :to)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByTypeAndDateRange(
            @Param("type") Transaction.Type type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND LOWER(t.category) = LOWER(:category)
        AND (:from IS NULL OR t.date >= :from)
        AND (:to   IS NULL OR t.date <= :to)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByCategoryAndDateRange(
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t JOIN FETCH t.createdBy
        WHERE t.deleted = false
        AND t.type = :type
        AND LOWER(t.category) = LOWER(:category)
        AND (:from IS NULL OR t.date >= :from)
        AND (:to   IS NULL OR t.date <= :to)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    Page<Transaction> findByTypeAndCategoryAndDateRange(
            @Param("type") Transaction.Type type,
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // ── Single record (non-deleted) ────────────────────────────────────────────

    @Query("SELECT t FROM Transaction t JOIN FETCH t.createdBy WHERE t.id = :id AND t.deleted = false")
    Optional<Transaction> findByIdAndNotDeleted(@Param("id") String id);

    // ── Aggregation totals ─────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.deleted = false AND t.type = :type")
    BigDecimal sumByType(@Param("type") Transaction.Type type);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.deleted = false")
    long countActive();

    // ── Category totals ────────────────────────────────────────────────────────

    @Query("""
        SELECT t.category, SUM(t.amount)
        FROM Transaction t
        WHERE t.deleted = false AND t.type = :type
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
    """)
    List<Object[]> categoryTotalsByType(@Param("type") Transaction.Type type);

    // ── Monthly trends (native SQL) ────────────────────────────────────────────

    @Query(value = """
        SELECT
            EXTRACT(YEAR  FROM date) AS yr,
            EXTRACT(MONTH FROM date) AS mo,
            type,
            SUM(amount) AS total
        FROM transactions
        WHERE deleted = false
          AND EXTRACT(YEAR FROM date) = :year
        GROUP BY yr, mo, type
        ORDER BY mo
    """, nativeQuery = true)
    List<Object[]> monthlyTrendsByYear(@Param("year") int year);

    // ── Recent transactions ────────────────────────────────────────────────────

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.createdBy
        WHERE t.deleted = false
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findRecentTransactions(Pageable pageable);
}