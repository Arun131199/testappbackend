package com.vayuratha.test.repository;

import com.vayuratha.test.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion,Long> {
    List<ExamQuestion> findByExamId(Long examId);
    boolean existsByExamIdAndQuestionId(Long examId, Long questionId);
    void deleteByExamIdAndQuestionId(Long examId, Long questionId);
    long countByExamId(Long examId);
}
