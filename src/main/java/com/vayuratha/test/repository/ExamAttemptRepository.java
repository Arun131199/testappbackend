package com.vayuratha.test.repository;

import com.vayuratha.test.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,Long> {
    List<ExamAttempt> findByUserId(String userId);
    Optional<ExamAttempt> findByIdAndUserId(Long id, String userId);
    List<ExamAttempt> findAllByOrderBySubmittedAtDesc();
    Optional<ExamAttempt> findByExamIdAndUserId(Long examId, String userId);
}
