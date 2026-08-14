package com.vayuratha.test.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "exam_attempts")
public class ExamAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Builder.Default
    private Integer violationCount = 0;

    private Integer score;

    private Integer totalMarks;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant submittedAt;   // renamed from endedAt

    public enum AttemptStatus {
        IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED
    }

    @PrePersist
    void prePersist() {
        if (startedAt == null) startedAt = Instant.now();
        if (status == null) status = AttemptStatus.IN_PROGRESS;
    }
}