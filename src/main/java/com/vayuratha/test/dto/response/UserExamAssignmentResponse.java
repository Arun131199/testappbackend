package com.vayuratha.test.dto.response;

import com.vayuratha.test.entity.Exam;

import java.time.Instant;

public record UserExamAssignmentResponse(
        Long examId,
        String title,
        String category,
        Exam.ExamStatus status,
        Integer questionCount,
        Integer durationMinutes,
        boolean assigned,
        boolean completed,
        Long attemptId,
        Instant assignedAt
) {
}