package com.vayuratha.test.service;

import com.vayuratha.test.dto.response.AdminStatsResponse;
import com.vayuratha.test.entity.*;
import com.vayuratha.test.repository.*;
import com.vayuratha.test.roleEnum.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAdminService {

    private static final long DEFAULT_LIVE_WINDOW_DAYS = 365;

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final UserRepository userRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;

    public Exam createExam(Exam exam) {
        Instant now = Instant.now();
        exam.setStatus(Exam.ExamStatus.LIVE);
        exam.setStartTime(now);
        exam.setEndTime(now.plus(DEFAULT_LIVE_WINDOW_DAYS, ChronoUnit.DAYS));
        return examRepository.save(exam);
    }

    public Exam publishExam(Long examId, Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (!endTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException("End time must be in the future");
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.LIVE);
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        return examRepository.save(exam);
    }

    public Exam closeExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.CLOSED);
        return examRepository.save(exam);
    }

    public String assignToUsers(Long examId, List<String> userIds) {
        int count = 0;
        for (String userId : userIds) {
            if (!examAssignmentRepository.existsByExamIdAndUserId(examId, userId)) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(userId).build()
                );
                count++;
            }
        }
        return "Assigned to " + count + " new student(s)";
    }

    public String assignToAllStudents(Long examId) {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .toList();

        int count = 0;
        for (User student : students) {
            if (!examAssignmentRepository.existsByExamIdAndUserId(examId, student.getUserId())) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(student.getUserId()).build()
                );
                count++;
            }
        }
        return "Assigned to " + count + " new student(s) out of " + students.size() + " total";
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public List<Exam> getLiveExams() {
        return examRepository.findByStatus(Exam.ExamStatus.LIVE);
    }

    // ===== Exam <-> Question assignment =====

    public String addQuestionsToExam(Long examId, List<Long> questionIds) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        int added = 0;
        for (Long questionId : questionIds) {
            if (!questionRepository.existsById(questionId)) continue;
            if (!examQuestionRepository.existsByExamIdAndQuestionId(examId, questionId)) {
                examQuestionRepository.save(
                        ExamQuestion.builder().examId(examId).questionId(questionId).build()
                );
                added++;
            }
        }

        long total = examQuestionRepository.countByExamId(examId);
        exam.setQuestionCount((int) total);
        examRepository.save(exam);

        return "Added " + added + " question(s). Exam now has " + total + " question(s) total.";
    }

    public String removeQuestionFromExam(Long examId, Long questionId) {
        examQuestionRepository.deleteByExamIdAndQuestionId(examId, questionId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        long total = examQuestionRepository.countByExamId(examId);
        exam.setQuestionCount((int) total);
        examRepository.save(exam);

        return "Removed. Exam now has " + total + " question(s) total.";
    }

    public List<Question> getQuestionsForExam(Long examId) {
        List<Long> questionIds = examQuestionRepository.findByExamId(examId).stream()
                .map(ExamQuestion::getQuestionId)
                .toList();
        return questionRepository.findAllById(questionIds);
    }

    public String addQuestionsByCategory(Long examId, String category, int count) {
        List<Question> pool = questionRepository.findByCategoryAndActiveTrue(category);
        if (pool.size() < count) {
            throw new IllegalStateException("Not enough questions in category: " + category);
        }
        Collections.shuffle(pool);
        List<Long> selectedIds = pool.subList(0, count).stream().map(Question::getId).toList();
        return addQuestionsToExam(examId, selectedIds);
    }

    // ===== Stats =====

    public AdminStatsResponse getStats() {
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .count();

        long activeExams = examRepository.findByStatus(Exam.ExamStatus.LIVE).size();

        List<ExamAttempt> allAttempts = examAttemptRepository.findAll();
        List<ExamAttempt> submitted = allAttempts.stream()
                .filter(a -> a.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS)
                .toList();

        long passed = submitted.stream()
                .filter(a -> a.getScore() != null && a.getTotalMarks() != null
                        && a.getScore() >= (a.getTotalMarks() * 0.5))
                .count();
        double overallPassPct = submitted.isEmpty() ? 0 : (passed * 100.0 / submitted.size());

        Map<String, List<ExamAttempt>> byCategory = submitted.stream()
                .collect(Collectors.groupingBy(ExamAttempt::getCategory));

        List<AdminStatsResponse.CategoryStat> categoryStats = byCategory.entrySet().stream()
                .map(e -> {
                    List<ExamAttempt> list = e.getValue();
                    long catPassed = list.stream()
                            .filter(a -> a.getScore() != null && a.getTotalMarks() != null
                                    && a.getScore() >= (a.getTotalMarks() * 0.5))
                            .count();
                    double pct = list.isEmpty() ? 0 : (catPassed * 100.0 / list.size());
                    return new AdminStatsResponse.CategoryStat(e.getKey(), list.size(), pct);
                }).toList();

        return new AdminStatsResponse(totalStudents, activeExams, overallPassPct, categoryStats);
    }
}