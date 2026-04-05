package com.finance.dashboard.service;

import com.finance.dashboard.dto.TransactionRequest;
import com.finance.dashboard.dto.TransactionResponse;
import com.finance.dashboard.exception.BadRequestException;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    // ── List with filtering + pagination ──────────────────────────────────────

    public Page<TransactionResponse> list(
            String category,
            String type,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        // Resolve type enum — throws 400 if value is unrecognised
        Transaction.Type typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = Transaction.Type.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid type '" + type + "'. Must be INCOME or EXPENSE");
            }
        }

        boolean hasType     = typeEnum != null;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDate     = from != null || to != null;

        // Route to the right query based on which filters are actually present.
        // This avoids passing nulls alongside enums in a single JPQL query,
        // which caused PostgreSQL to error with "function lower(bytea) does not exist".
        Page<Transaction> page;

        if (hasType && hasCategory && hasDate) {
            page = transactionRepository.findByTypeAndCategoryAndDateRange(typeEnum, category, from, to, pageable);
        } else if (hasType && hasCategory) {
            page = transactionRepository.findByTypeAndCategory(typeEnum, category, pageable);
        } else if (hasType && hasDate) {
            page = transactionRepository.findByTypeAndDateRange(typeEnum, from, to, pageable);
        } else if (hasCategory && hasDate) {
            page = transactionRepository.findByCategoryAndDateRange(category, from, to, pageable);
        } else if (hasType) {
            page = transactionRepository.findByType(typeEnum, pageable);
        } else if (hasCategory) {
            page = transactionRepository.findByCategory(category, pageable);
        } else if (hasDate) {
            page = transactionRepository.findByDateRange(from, to, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }

        List<TransactionResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    // ── Get single transaction ─────────────────────────────────────────────────

    public TransactionResponse getById(String id) {
        return toResponse(findActive(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse create(TransactionRequest request, String creatorEmail) {
        User creator = userService.findByEmail(creatorEmail);

        Transaction tx = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(creator)
                .deleted(false)
                .build();

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction created: {} {} {} by {}",
                saved.getType(), saved.getAmount(), saved.getCategory(), creatorEmail);
        return toResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse update(String id, TransactionRequest request) {
        Transaction tx = findActive(id);

        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setCategory(request.getCategory().trim());
        tx.setDate(request.getDate());
        tx.setNotes(request.getNotes());

        Transaction updated = transactionRepository.save(tx);
        log.info("Transaction updated: {}", id);
        return toResponse(updated);
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        Transaction tx = findActive(id);
        tx.setDeleted(true);
        transactionRepository.save(tx);
        log.info("Transaction soft-deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction findActive(String id) {
        return transactionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
    }

    public TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .type(tx.getType().name())
                .category(tx.getCategory())
                .date(tx.getDate())
                .notes(tx.getNotes())
                .createdById(tx.getCreatedBy().getId())
                .createdByName(tx.getCreatedBy().getFullName())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}