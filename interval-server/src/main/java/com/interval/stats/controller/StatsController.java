package com.interval.stats.controller;

import com.interval.auth.security.AuthenticatedUser;
import com.interval.common.dto.ApiResponse;
import com.interval.common.exception.GlobalExceptionHandler;
import com.interval.stats.dto.CategoryDurationSummaryDto;
import com.interval.stats.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/category-durations")
    public ResponseEntity<ApiResponse<?>> getCategoryDurations(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            CategoryDurationSummaryDto summary = statsService.getCategoryDurations(user.userId(), startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Category duration statistics retrieved successfully", summary));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
