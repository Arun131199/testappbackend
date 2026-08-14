package com.vayuratha.test.controller;

import com.vayuratha.test.entity.*;
import com.vayuratha.test.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/results")
@RequiredArgsConstructor
public class AdminResultController {

    private final ExamAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> allResults() {
        List<ExamAttempt> attempts = attemptRepository.findAllByOrderBySubmittedAtDesc();
        List<Map<String, Object>> enriched = attempts.stream().map(this::toResultMap).toList();
        return ResponseEntity.ok(enriched);
    }

    /** Download the same rows shown in the admin results screen as an Excel file. */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportResults() {
        List<Map<String, Object>> results = attemptRepository.findAllByOrderBySubmittedAtDesc()
                .stream().map(this::toResultMap).toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Admin Results");
            String[] headers = {"Attempt ID", "Student Name", "Student ID", "Exam", "Category", "Score",
                    "Total Marks", "Percentage", "Result", "Status", "Answered", "Correct", "Wrong",
                    "Violations", "Time Taken (minutes)", "Started At", "Submitted At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            for (int rowIndex = 0; rowIndex < results.size(); rowIndex++) {
                Map<String, Object> result = results.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = {result.get("attemptId"), result.get("studentName"), result.get("studentId"),
                        result.get("examTitle"), result.get("category"), result.get("score"), result.get("totalMarks"),
                        result.get("percentage"), Boolean.TRUE.equals(result.get("passed")) ? "PASS" : "FAIL",
                        result.get("status"), result.get("answeredCount"), result.get("correctCount"), result.get("wrongCount"),
                        result.get("violationCount"), result.get("timeTakenMinutes"), result.get("startedAt"), result.get("submittedAt")};
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column] == null ? "" : String.valueOf(values[column]));
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(output);
            ByteArrayResource file = new ByteArrayResource(output.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-results.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(file.contentLength())
                    .body(file);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not export admin results", exception);
        }
    }

    /**
     * Open one student's attempt from the admin results table.
     * Each question includes the chosen option and the correct option for review.
     */
    @GetMapping("/{attemptId}")
    public ResponseEntity<Map<String, Object>> resultDetails(@PathVariable Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Attempt not found: " + attemptId));

        Map<String, Object> detail = new LinkedHashMap<>(toResultMap(attempt));
        List<Long> questionIds = questionIds(attempt);
        Map<Long, Question> questions = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));
        Map<Long, String> answers = attemptAnswerRepository.findByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption));

        List<Map<String, Object>> questionDetails = new ArrayList<>();
        int number = 1;
        for (Long questionId : questionIds) {
            Question question = questions.get(questionId);
            if (question == null) continue;
            String selectedOption = answers.get(questionId);
            Map<String, Object> questionDetail = new LinkedHashMap<>();
            questionDetail.put("number", number++);
            questionDetail.put("questionId", questionId);
            questionDetail.put("questionText", question.getQuestionText());
            questionDetail.put("options", Map.of("A", question.getOptionA(), "B", question.getOptionB(),
                    "C", question.getOptionC(), "D", question.getOptionD()));
            questionDetail.put("selectedOption", selectedOption);
            questionDetail.put("selectedAnswer", optionText(question, selectedOption));
            questionDetail.put("correctOption", question.getCorrectAnswer());
            questionDetail.put("correctAnswer", optionText(question, question.getCorrectAnswer()));
            questionDetail.put("answered", selectedOption != null);
            questionDetail.put("correct", selectedOption != null && question.getCorrectAnswer().equalsIgnoreCase(selectedOption));
            questionDetail.put("marks", question.getMarks());
            questionDetails.add(questionDetail);
        }
        detail.put("questions", questionDetails);
        return ResponseEntity.ok(detail);
    }

    private Map<String, Object> toResultMap(ExamAttempt attempt) {
        User user = userRepository.findByUserId(attempt.getUserId()).orElse(null);

        Exam exam = examRepository.findById(attempt.getExamId()).orElse(null);

        List<Long> questionIds = questionIds(attempt);

        List<Question> questions = questionRepository.findAllById(questionIds);
        Map<Long, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<AttemptAnswer> savedAnswers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        Map<Long, String> answersMap = savedAnswers.stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption));

        int totalQuestions = questionIds.size();
        int correctCount = 0;
        int answeredCount = 0;

        for (Long qId : questionIds) {
            String given = answersMap.get(qId);
            if (given != null) {
                answeredCount++;
                Question q = qMap.get(qId);
                if (q != null && q.getCorrectAnswer().equalsIgnoreCase(given)) {
                    correctCount++;
                }
            }
        }
        int wrongCount = answeredCount - correctCount;

        double percentage = 0.0;
        if (attempt.getScore() != null && attempt.getTotalMarks() != null && attempt.getTotalMarks() > 0) {
            percentage = Math.round((attempt.getScore() * 10000.0) / attempt.getTotalMarks()) / 100.0;
        }
        boolean passed = percentage >= 50.0;

        long timeTakenMinutes = 0;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            timeTakenMinutes = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMinutes();
        }

        Map<String, Object> row = new HashMap<>();
        row.put("attemptId", attempt.getId());
        row.put("studentName", user != null ? user.getFullName() : "Unknown");
        row.put("studentId", attempt.getUserId());
        row.put("examTitle", exam != null ? exam.getTitle() : "Unknown");
        row.put("category", attempt.getCategory());
        row.put("score", attempt.getScore());
        row.put("totalMarks", attempt.getTotalMarks());
        row.put("percentage", percentage);
        row.put("passed", passed);
        row.put("status", attempt.getStatus());
        row.put("violationCount", attempt.getViolationCount());
        row.put("totalQuestions", totalQuestions);
        row.put("answeredCount", answeredCount);
        row.put("correctCount", correctCount);
        row.put("wrongCount", wrongCount);
        row.put("timeTakenMinutes", timeTakenMinutes);
        row.put("startedAt", attempt.getStartedAt());
        row.put("submittedAt", attempt.getSubmittedAt());

        return row;
    }

    private List<Long> questionIds(ExamAttempt attempt) {
        if (attempt.getQuestionOrder() == null || attempt.getQuestionOrder().isBlank()) return List.of();
        return Arrays.stream(attempt.getQuestionOrder().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::parseLong)
                .toList();
    }

    private String optionText(Question question, String option) {
        if (option == null) return null;
        return switch (option.toUpperCase(Locale.ROOT)) {
            case "A" -> question.getOptionA();
            case "B" -> question.getOptionB();
            case "C" -> question.getOptionC();
            case "D" -> question.getOptionD();
            default -> null;
        };
    }
}
