package com.vayuratha.test.controller;

import com.vayuratha.test.dto.respoonse.AdminStatsResponse;
import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.service.ExamAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private final ExamAdminService examAdminService;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        return ResponseEntity.ok(examAdminService.createExam(exam));
    }

    // Accepts startTime/endTime as IST wall-clock strings, e.g. "2026-08-13T15:05:00" (no Z suffix)
    @PutMapping("/{examId}/publish")
    public ResponseEntity<Exam> publishExam(@PathVariable Long examId, @RequestBody Map<String, String> body) {
        LocalDateTime startLocal = LocalDateTime.parse(body.get("startTime"), FORMATTER);
        LocalDateTime endLocal = LocalDateTime.parse(body.get("endTime"), FORMATTER);

        Instant start = ZonedDateTime.of(startLocal, IST_ZONE).toInstant();
        Instant end = ZonedDateTime.of(endLocal, IST_ZONE).toInstant();

        return ResponseEntity.ok(examAdminService.publishExam(examId, start, end));
    }

    @PutMapping("/{examId}/close")
    public ResponseEntity<Exam> closeExam(@PathVariable Long examId) {
        return ResponseEntity.ok(examAdminService.closeExam(examId));
    }

    @PostMapping("/{examId}/assign")
    public ResponseEntity<String> assignToUsers(@PathVariable Long examId, @RequestBody List<String> userIds) {
        return ResponseEntity.ok(examAdminService.assignToUsers(examId, userIds));
    }

    @PostMapping("/{examId}/assign-all")
    public ResponseEntity<String> assignToAll(@PathVariable Long examId) {
        return ResponseEntity.ok(examAdminService.assignToAllStudents(examId));
    }

    @GetMapping
    public ResponseEntity<List<Exam>> getAllExams() {
        return ResponseEntity.ok(examAdminService.getAllExams());
    }

    @GetMapping("/live")
    public ResponseEntity<List<Exam>> getLiveExams() {
        return ResponseEntity.ok(examAdminService.getLiveExams());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(examAdminService.getStats());
    }
}
