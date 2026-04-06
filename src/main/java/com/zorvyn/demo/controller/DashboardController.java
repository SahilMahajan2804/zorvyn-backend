package com.zorvyn.demo.controller;

import com.zorvyn.demo.dto.*;
import com.zorvyn.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final RecordService recordService;

    /**
     * GET /api/dashboard/summary
     * All roles — total income, expense, net balance, record count.
     */
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getSummary(userId, auth));
    }

    /**
     * GET /api/dashboard/categories
     * ANALYST, ADMIN — category-wise income/expense totals.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryTotal>> getCategories(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getCategoryTotals(userId, auth));
    }

    /**
     * GET /api/dashboard/trends
     * ANALYST, ADMIN — monthly totals for last 12 months.
     */
    @GetMapping("/trends")
    public ResponseEntity<List<MonthlyTrend>> getTrends(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends(userId, auth));
    }

    /**
     * GET /api/dashboard/recent
     * All roles — 10 most recent records.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<RecordDto>> getRecent(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getRecentActivity(recordService, userId, auth));
    }
}
