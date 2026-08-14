package com.vayuratha.test.controller;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamAttempt;
import com.vayuratha.test.repository.ExamAttemptRepository;
import com.vayuratha.test.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/results")
public class ResultController {
    private final ExamAttemptRepository attemptRepository;
    private final ExamRepository examRepository;

    @GetMapping("/me")
    public ResponseEntity<List<Map<String, Object>>> myResults(Authentication authentication) {
        String userId = authentication.getName();
        List<ExamAttempt> attempts = attemptRepository.findByUserId(userId);

        List<Map<String, Object>> enriched = attempts.stream().map(this::toResultMap).toList();
        return ResponseEntity.ok(enriched);
    }

    private Map<String, Object> toResultMap(ExamAttempt attempt) {
        Exam exam = examRepository.findById(attempt.getExamId()).orElse(null);

        double percentage = 0.0;
        if (attempt.getScore() != null && attempt.getTotalMarks() != null && attempt.getTotalMarks() > 0) {
            percentage = Math.round((attempt.getScore() * 10000.0) / attempt.getTotalMarks()) / 100.0;
        }

        boolean passed = percentage >= 50.0;

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("examTitle", exam != null ? exam.getTitle() : "Unknown");
        result.put("category", attempt.getCategory());
        result.put("score", attempt.getScore());
        result.put("totalMarks", attempt.getTotalMarks());
        result.put("percentage", percentage);
        result.put("passed", passed);
        result.put("status", attempt.getStatus());
        result.put("violationCount", attempt.getViolationCount());
        result.put("startedAt", attempt.getStartedAt());
        result.put("submittedAt", attempt.getSubmittedAt());

        return result;
    }
}