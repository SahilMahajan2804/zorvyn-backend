package com.zorvyn.demo.service;

import com.zorvyn.demo.dto.*;
import com.zorvyn.demo.entity.RecordType;
import com.zorvyn.demo.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final com.zorvyn.demo.repository.UserRepository userRepository;

    // ── Helper for User Scoping ──────────────────────────────────────────────
    private Long resolveUserId(Authentication auth, Long requestedUserId) {
        if (auth == null) return requestedUserId;
        boolean isViewer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
        
        if (isViewer) {
            return userRepository.findByEmailAndDeletedAtIsNull(auth.getName())
                    .map(com.zorvyn.demo.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        return requestedUserId; // Analyst or Admin can pass null for all, or specific userId
    }

    // ── Summary (all roles) ──────────────────────────────────────────────────

    public SummaryResponse getSummary(Long targetUserId, Authentication auth) {
        Long resolvedUserId = resolveUserId(auth, targetUserId);
        List<Object[]> rows = recordRepository.sumByType(resolvedUserId);

        BigDecimal income  = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Object[] row : rows) {
            RecordType type  = (RecordType) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (type == RecordType.INCOME)  income  = total;
            if (type == RecordType.EXPENSE) expense = total;
        }

        long totalRecords = recordRepository.count();

        return SummaryResponse.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .netBalance(income.subtract(expense))
                .totalRecords(totalRecords)
                .build();
    }

    // ── Category totals (ANALYST, ADMIN) ────────────────────────────────────

    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public List<CategoryTotal> getCategoryTotals(Long targetUserId, Authentication auth) {
        Long resolvedUserId = resolveUserId(auth, targetUserId);
        return recordRepository.sumByCategoryAndType(resolvedUserId)
                .stream()
                .map(row -> CategoryTotal.builder()
                        .category((String) row[0])
                        .type(((RecordType) row[1]).name())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Monthly trends — last 12 months (ANALYST, ADMIN) ────────────────────

    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public List<MonthlyTrend> getMonthlyTrends(Long targetUserId, Authentication auth) {
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusMonths(11).withDayOfMonth(1);
        Long resolvedUserId = resolveUserId(auth, targetUserId);

        return recordRepository.monthlyTrends(from, to, resolvedUserId)
                .stream()
                .map(row -> MonthlyTrend.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .type(((RecordType) row[2]).name())
                        .total((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Recent activity (all roles) ──────────────────────────────────────────

    public List<RecordDto> getRecentActivity(RecordService recordService, Long targetUserId, Authentication auth) {
        Long resolvedUserId = resolveUserId(auth, targetUserId);
        return recordRepository
                .findRecentFiltered(resolvedUserId,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .map(recordService::toDto)
                .collect(Collectors.toList());
    }
}
