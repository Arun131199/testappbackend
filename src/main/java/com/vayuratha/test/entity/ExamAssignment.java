package com.vayuratha.test.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exam_assignments")
public class ExamAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private String userId;

    @Builder.Default
    private Boolean completed = false;

    private Long attemptId;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @PrePersist
    void prePersist() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
