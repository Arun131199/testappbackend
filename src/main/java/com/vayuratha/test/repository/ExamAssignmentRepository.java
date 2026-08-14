package com.vayuratha.test.repository;

import com.vayuratha.test.entity.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment,Long> {
    List<ExamAssignment> findByUserId(String userId);
    Optional<ExamAssignment> findByExamIdAndUserId(Long examId, String userId);
    boolean existsByExamIdAndUserId(Long examId, String userId);
    List<ExamAssignment> findByExamId(Long examId);
}
