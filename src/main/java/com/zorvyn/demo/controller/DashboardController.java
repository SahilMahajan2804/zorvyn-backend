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


    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getSummary(userId, auth));
    }


    @GetMapping("/categories")
    public ResponseEntity<List<CategoryTotal>> getCategories(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getCategoryTotals(userId, auth));
    }


    @GetMapping("/trends")
    public ResponseEntity<List<MonthlyTrend>> getTrends(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends(userId, auth));
    }


    @GetMapping("/recent")
    public ResponseEntity<List<RecordDto>> getRecent(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getRecentActivity(recordService, userId, auth));
    }
}
