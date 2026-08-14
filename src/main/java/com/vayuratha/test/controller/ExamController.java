package com.vayuratha.test.controller;

import com.vayuratha.test.entity.ExamAttempt;
import com.vayuratha.test.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/exam")
public class ExamController {
    private final ExamService examService;

    @PostMapping("/start")
    public ResponseEntity<ExamAttempt> start(Authentication authentication, @RequestParam Long examId) {
        String userId = authentication.getName();
        return ResponseEntity.ok(examService.startExam(userId, examId));
    }

    // Get a single question by index (0-based) - for next/prev navigation
    @GetMapping("/{attemptId}/question/{index}")
    public ResponseEntity<Map<String, Object>> getQuestion(Authentication authentication,
                                                            @PathVariable Long attemptId,
                                                            @PathVariable int index) {
        return ResponseEntity.ok(examService.getQuestionByIndex(authentication.getName(), attemptId, index));
    }

    // Save one answer as the student selects it
    @PostMapping("/{attemptId}/answer")
    public ResponseEntity<Void> saveAnswer(Authentication authentication,
                                           @PathVariable Long attemptId,
                                           @RequestBody Map<String, String> body) {
        Long questionId = Long.parseLong(body.get("questionId"));
        String selectedOption = body.get("selectedOption");
        examService.saveAnswer(authentication.getName(), attemptId, questionId, selectedOption);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{attemptId}/violation")
    public ResponseEntity<ExamAttempt> violation(Authentication authentication, @PathVariable Long attemptId) {
        return ResponseEntity.ok(examService.registerViolation(authentication.getName(), attemptId));
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ExamAttempt> submit(Authentication authentication,
                                              @PathVariable Long attemptId,
                                              @RequestBody(required = false) Map<Long, String> answers) {
        return ResponseEntity.ok(examService.submitExam(authentication.getName(), attemptId, answers));
    }
}
