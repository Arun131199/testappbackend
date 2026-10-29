package com.vayuratha.test.dto.response;

import java.time.Instant;

public record ExamAssigneeResponse(
        String userId,
        String fullName,
        String email,
        String mobile,
        boolean completed,
        Long attemptId,
        Instant assignedAt
) {
}