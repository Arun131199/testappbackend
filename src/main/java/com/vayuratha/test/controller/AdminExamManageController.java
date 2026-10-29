package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.AssignExamsRequest;
import com.vayuratha.test.dto.request.AssignUsersRequest;
import com.vayuratha.test.dto.request.ExamUpdateRequest;
import com.vayuratha.test.dto.response.ExamAssigneeResponse;
import com.vayuratha.test.dto.response.UserExamAssignmentResponse;
import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.service.ExamManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminExamManageController {

    private final ExamManageService examManageService;

    // ===== Exam edit =====

    @GetMapping("/exams/{examId}")
    public ResponseEntity<Exam> getExam(@PathVariable Long examId) {
        return ResponseEntity.ok(examManageService.getExam(examId));
    }

    @PutMapping("/exams/{examId}")
    public ResponseEntity<Exam> updateExam(@PathVariable Long examId,
                                           @RequestBody ExamUpdateRequest request) {
        return ResponseEntity.ok(examManageService.updateExam(examId, request));
    }


    @GetMapping("/exams/{examId}/assignments")
    public ResponseEntity<List<ExamAssigneeResponse>> getAssignees(@PathVariable Long examId) {
        return ResponseEntity.ok(examManageService.getAssignees(examId));
    }

    @PutMapping("/exams/{examId}/assignments")
    public ResponseEntity<Map<String, Object>> syncUsersForExam(@PathVariable Long examId,
                                                                @RequestBody AssignUsersRequest request) {
        return ResponseEntity.ok(examManageService.syncUsersForExam(examId, request.userIds()));
    }

    @DeleteMapping("/exams/{examId}/assignments/{userId}")
    public ResponseEntity<Map<String, String>> unassign(@PathVariable Long examId,
                                                        @PathVariable String userId) {
        return ResponseEntity.ok(Map.of("message", examManageService.unassign(examId, userId)));
    }


    @GetMapping("/users/{userId}/exam-assignments")
    public ResponseEntity<List<UserExamAssignmentResponse>> getExamsForUser(@PathVariable String userId) {
        return ResponseEntity.ok(examManageService.getExamsForUser(userId));
    }

    @PutMapping("/users/{userId}/exam-assignments")
    public ResponseEntity<Map<String, Object>> syncExamsForUser(@PathVariable String userId,
                                                                @RequestBody AssignExamsRequest request) {
        return ResponseEntity.ok(examManageService.syncExamsForUser(userId, request.examIds()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }
}