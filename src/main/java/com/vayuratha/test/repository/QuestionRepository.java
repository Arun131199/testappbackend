package com.vayuratha.test.repository;

import com.vayuratha.test.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question,Long> {
    List<Question> findByCategoryAndActiveTrue(String category);
    List<Question> findByCategory(String category);
}
