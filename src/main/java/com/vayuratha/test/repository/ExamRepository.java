package com.vayuratha.test.repository;

import com.vayuratha.test.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam,Long> {
    List<Exam> findByStatus(Exam.ExamStatus status);
    List<Exam> findByStatusAndIsDefaultForNewUsers(Exam.ExamStatus status,Boolean isDefaultForNewUsers);
}
