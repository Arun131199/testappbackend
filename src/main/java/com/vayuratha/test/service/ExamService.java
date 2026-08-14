package com.vayuratha.test.service;

import com.vayuratha.test.dto.respoonse.QuestionResponse;
import com.vayuratha.test.entity.*;
import com.vayuratha.test.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {
    private static final SecureRandom QUESTION_RANDOM = new SecureRandom();

    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    @Value("${app.exam.max-violations-before-auto-submit:3}")
    private int maxViolations;

    public ExamAttempt startExam(String userId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (exam.getStatus() != Exam.ExamStatus.LIVE) {
            throw new IllegalStateException("Exam is not live");
        }

        if (exam.getEndTime() != null && Instant.now().isAfter(exam.getEndTime())) {
            throw new IllegalStateException("Exam time window has ended");
        }

        ExamAssignment assignment = examAssignmentRepository.findByExamIdAndUserId(examId, userId)
                .orElseThrow(() -> new IllegalStateException("This exam is not assigned to you"));

        if (Boolean.TRUE.equals(assignment.getCompleted())) {
            throw new IllegalStateException("You have already completed this exam");
        }

        // A retry/refresh must resume the same attempt so its question order never changes mid-exam.
        if (assignment.getAttemptId() != null) {
            Optional<ExamAttempt> existingAttempt = attemptRepository.findById(assignment.getAttemptId());
            if (existingAttempt.isPresent()
                    && existingAttempt.get().getStatus() == ExamAttempt.AttemptStatus.IN_PROGRESS) {
                return existingAttempt.get();
            }
        }

        List<Question> pool = questionRepository.findByCategoryAndActiveTrue(exam.getCategory());
        if (pool.size() < exam.getQuestionCount()) {
            throw new IllegalStateException("Not enough questions in bank for category: " + exam.getCategory());
        }

        Collections.shuffle(pool, QUESTION_RANDOM);

        List<Question> selected = pool.subList(0, exam.getQuestionCount());
        String order = selected.stream()
                .map(q -> q.getId().toString())
                .collect(Collectors.joining(","));

        int totalMarks = selected.stream().mapToInt(Question::getMarks).sum();

        ExamAttempt attempt = ExamAttempt.builder()
                .userId(userId)
                .examId(examId)
                .category(exam.getCategory())
                .questionOrder(order)
                .totalMarks(totalMarks)
                .status(ExamAttempt.AttemptStatus.IN_PROGRESS)
                .build();

        ExamAttempt saved = attemptRepository.save(attempt);

        assignment.setAttemptId(saved.getId());
        examAssignmentRepository.save(assignment);

        return saved;
    }

    // Get ONE question by index (0-based). userId-scoped for security; correctAnswer is never returned.
    public Map<String, Object> getQuestionByIndex(String userId, Long attemptId, int index) {
        ExamAttempt attempt = getAttempt(userId, attemptId);

        List<Long> ids = Arrays.stream(attempt.getQuestionOrder().split(","))
                .map(Long::parseLong).toList();

        if (index < 0 || index >= ids.size()) {
            throw new IllegalArgumentException("Invalid question index");
        }

        Long questionId = ids.get(index);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        // Hide correctAnswer from the response
        QuestionResponse safeQuestion = new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getCategory(),
                question.getMarks()
        );

        String selectedOption = attemptAnswerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .map(AttemptAnswer::getSelectedOption)
                .orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("index", index);
        result.put("totalQuestions", ids.size());
        result.put("question", safeQuestion);   // safe DTO, no correctAnswer
        result.put("selectedOption", selectedOption);
        result.put("examEndTime", getExamEndTime(attempt));

        return result;
    }

    // Save/update a single answer as the user selects it
    public void saveAnswer(String userId, Long attemptId, Long questionId, String selectedOption) {
        ExamAttempt attempt = getAttempt(userId, attemptId);

        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Exam is already submitted");
        }

        if (hasExamEnded(attempt)) {
            throw new IllegalStateException("Exam has ended; answers can no longer be changed. Please submit your exam.");
        }

        AttemptAnswer answer = attemptAnswerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElse(AttemptAnswer.builder()
                        .attemptId(attemptId)
                        .questionId(questionId)
                        .build());

        answer.setSelectedOption(selectedOption);
        attemptAnswerRepository.save(answer);
    }

    private ExamAttempt getAttempt(String userId, Long attemptId) {
        return attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found for this user"));
    }

    private boolean hasExamEnded(ExamAttempt attempt) {
        Instant endTime = getExamEndTime(attempt);
        return endTime != null && !Instant.now().isBefore(endTime);
    }

    private Instant getExamEndTime(ExamAttempt attempt) {
        return examRepository.findById(attempt.getExamId())
                .map(Exam::getEndTime)
                .orElse(null);
    }

    public ExamAttempt registerViolation(String userId, Long attemptId) {
        ExamAttempt attempt = getAttempt(userId, attemptId);

        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS) return attempt;
        if (hasExamEnded(attempt)) return attempt;
        attempt.setViolationCount(attempt.getViolationCount() + 1);

        if (attempt.getViolationCount() >= maxViolations) {
            List<AttemptAnswer> savedAnswers = attemptAnswerRepository.findByAttemptId(attemptId);
            Map<Long, String> answersMap = savedAnswers.stream()
                    .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption));
            return scoreAndFinish(attempt, answersMap, ExamAttempt.AttemptStatus.AUTO_SUBMITTED);
        }
        return attemptRepository.save(attempt);
    }

    // Manual submit by student (clicking Submit button)
    public ExamAttempt submitExam(String userId, Long attemptId, Map<Long, String> userAnswers) {
        ExamAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found for this user"));

        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS) {
            return attempt;
        }

        List<AttemptAnswer> savedAnswers = attemptAnswerRepository.findByAttemptId(attemptId);
        Map<Long, String> finalAnswers = new HashMap<>(savedAnswers.stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption)));
        if (userAnswers != null) {
            finalAnswers.putAll(userAnswers);
        }

        return scoreAndFinish(attempt, finalAnswers, ExamAttempt.AttemptStatus.SUBMITTED);
    }

    private ExamAttempt scoreAndFinish(ExamAttempt attempt, Map<Long, String> answers, ExamAttempt.AttemptStatus finalStatus) {
        List<Long> questionIds = Arrays.stream(attempt.getQuestionOrder().split(","))
                .map(Long::parseLong).toList();

        List<Question> questions = questionRepository.findAllById(questionIds);
        Map<Long, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int score = 0;
        for (Long qId : questionIds) {
            Question q = qMap.get(qId);
            String given = answers.get(qId);
            if (q != null && given != null && q.getCorrectAnswer().equalsIgnoreCase(given)) {
                score += q.getMarks();
            }
        }

        attempt.setScore(score);
        attempt.setStatus(finalStatus);
        attempt.setSubmittedAt(Instant.now());

        examAssignmentRepository.findByExamIdAndUserId(attempt.getExamId(), attempt.getUserId())
                .ifPresent(a -> {
                    a.setCompleted(true);
                    examAssignmentRepository.save(a);
                });

        return attemptRepository.save(attempt);
    }
}