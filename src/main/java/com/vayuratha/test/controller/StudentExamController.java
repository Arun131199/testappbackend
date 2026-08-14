package com.vayuratha.test.controller;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamAssignment;
import com.vayuratha.test.repository.ExamAssignmentRepository;
import com.vayuratha.test.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentExamController {
    private final ExamAssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;

    // Get exams assigned to this student, not yet completed
    @GetMapping("/exams/assigned/{userId}")
    public ResponseEntity<List<Exam>> getAssignedExams(@PathVariable String userId) {
        List<Long> examIds = assignmentRepository.findByUserId(userId).stream()
                .filter(a -> !a.getCompleted())
                .map(ExamAssignment::getExamId)
                .toList();

        List<Exam> exams = examRepository.findAllById(examIds);

        // only show if the exam is still LIVE
        List<Exam> liveOnly = exams.stream()
                .filter(e -> e.getStatus() == Exam.ExamStatus.LIVE)
                .toList();

        return ResponseEntity.ok(liveOnly);
    }

    // Get all exams (assigned + completed) for history view
    @GetMapping("/exams/history/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getExamHistory(@PathVariable String userId) {
        List<ExamAssignment> assignments = assignmentRepository.findByUserId(userId);

        List<Map<String, Object>> result = assignments.stream().map(a -> {
            Exam exam = examRepository.findById(a.getExamId()).orElse(null);
            return Map.<String, Object>of(
                    "examId", a.getExamId(),
                    "examTitle", exam != null ? exam.getTitle() : "Unknown",
                    "category", exam != null ? exam.getCategory() : "",
                    "completed", a.getCompleted(),
                    "attemptId", a.getAttemptId() != null ? a.getAttemptId() : 0
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

}
