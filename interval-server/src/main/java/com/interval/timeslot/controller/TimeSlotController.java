package com.interval.timeslot.controller;

import com.interval.common.dto.ApiResponse;
import com.interval.auth.security.AuthenticatedUser;
import com.interval.common.exception.GlobalExceptionHandler;
import com.interval.timeslot.dto.DeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchDeleteTimeSlotRequest;
import com.interval.timeslot.dto.BatchDeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchUpsertTimeSlotRequest;
import com.interval.timeslot.dto.BatchUpsertTimeSlotResponseDto;
import com.interval.timeslot.dto.TimeSlotDto;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;
import com.interval.timeslot.service.TimeSlotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-slots")
@Validated
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDailySlots(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<TimeSlotDto> slots = timeSlotService.getDailySlots(user.userId(), date);
            return ResponseEntity.ok(ApiResponse.success("Time slots retrieved successfully", slots));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<?>> upsertTimeSlot(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertTimeSlotRequest request) {
        try {
            TimeSlotDto slot = timeSlotService.upsertTimeSlot(user.userId(), request);
            return ResponseEntity.ok(ApiResponse.success("Time slot saved successfully", slot));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<?>> batchUpsertTimeSlots(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BatchUpsertTimeSlotRequest request) {
        try {
            BatchUpsertTimeSlotResponseDto result = timeSlotService.batchUpsertTimeSlots(user.userId(), request);
            return ResponseEntity.ok(ApiResponse.success("Time slots saved successfully", result));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<ApiResponse<?>> deleteTimeSlot(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive Long slotId) {
        try {
            DeleteTimeSlotResponseDto result = timeSlotService.deleteTimeSlot(user.userId(), slotId);
            return ResponseEntity.ok(ApiResponse.success("Time slot deleted successfully", result));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<?>> batchDeleteTimeSlots(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BatchDeleteTimeSlotRequest request) {
        try {
            BatchDeleteTimeSlotResponseDto result = timeSlotService.batchDeleteTimeSlots(user.userId(), request);
            return ResponseEntity.ok(ApiResponse.success("Time slots deleted successfully", result));
        } catch (IllegalArgumentException e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
