package com.vayuratha.test.service;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamQuestion;
import com.vayuratha.test.entity.Question;
import com.vayuratha.test.repository.ExamQuestionRepository;
import com.vayuratha.test.repository.ExamRepository;
import com.vayuratha.test.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    private final QuestionRepository questionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamRepository examRepository;

    @Transactional
    public ImportResult importFromExcel(MultipartFile file, Long examId, int questionsPerAttempt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty Excel file");
        }
        if (questionsPerAttempt <= 0) {
            throw new IllegalArgumentException("Questions per attempt must be greater than zero");
        }

        Exam exam = examId == null ? null : examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        List<Question> toSave = new ArrayList<>();
        int errorCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(0);
            int questionColumn = findColumn(header, formatter, evaluator, 0, "question", "questiontext");
            int optionAColumn = findColumn(header, formatter, evaluator, 1, "optiona", "a");
            int optionBColumn = findColumn(header, formatter, evaluator, 2, "optionb", "b");
            int optionCColumn = findColumn(header, formatter, evaluator, 3, "optionc", "c");
            int optionDColumn = findColumn(header, formatter, evaluator, 4, "optiond", "d");
            int answerColumn = findColumn(header, formatter, evaluator, 5, "correctanswer", "answer", "correctoption");
            int categoryColumn = findColumn(header, formatter, evaluator, 6, "category", "topic", "subject");
            int marksColumn = findColumn(header, formatter, evaluator, -1, "marks", "mark", "score");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String qText = getCell(row, questionColumn, formatter, evaluator);
                    String optA = getCell(row, optionAColumn, formatter, evaluator);
                    String optB = getCell(row, optionBColumn, formatter, evaluator);
                    String optC = getCell(row, optionCColumn, formatter, evaluator);
                    String optD = getCell(row, optionDColumn, formatter, evaluator);
                    String correct = normalizeCorrectAnswer(
                            getCell(row, answerColumn, formatter, evaluator),
                            optA,
                            optB,
                            optC,
                            optD
                    );
                    String category = getCell(row, categoryColumn, formatter, evaluator);
                    int marks = parseMarks(getCell(row, marksColumn, formatter, evaluator));

                    if (qText.isBlank() || !List.of("A", "B", "C", "D").contains(correct)) {
                        errorCount++;
                        continue;
                    }
                    toSave.add(Question.builder()
                            .questionText(qText)
                            .optionA(optA).optionB(optB).optionC(optC).optionD(optD)
                            .correctAnswer(correct)
                            .category(category.isBlank() ? "General" : category)
                            .marks(marks)
                            .active(true)
                            .build());

                } catch (Exception e) {
                    errorCount++;
                }
            }
        }

        if (toSave.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid questions were found in the Excel file. Failed rows: " + errorCount
            );
        }

        List<Question> saved = questionRepository.saveAllAndFlush(toSave);

        if (examId != null) {
            Set<Long> linkedQuestionIds = new HashSet<>(examQuestionRepository.findByExamId(examId).stream()
                    .map(ExamQuestion::getQuestionId)
                    .toList());
            List<ExamQuestion> links = saved.stream()
                    .filter(question -> !linkedQuestionIds.contains(question.getId()))
                    .map(question -> ExamQuestion.builder()
                            .examId(examId)
                            .questionId(question.getId())
                            .build())
                    .toList();
            examQuestionRepository.saveAllAndFlush(links);

            exam.setQuestionCount(questionsPerAttempt);
            examRepository.save(exam);

            long poolSize = linkedQuestionIds.size() + links.size();
            return new ImportResult(saved.size(), errorCount, poolSize, questionsPerAttempt);
        }

        return new ImportResult(saved.size(), errorCount, null, null);
    }

    private String getCell(Row row, int idx, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (idx < 0) return "";
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private int findColumn(
            Row header,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            int fallback,
            String... aliases
    ) {
        if (header == null) return fallback;
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell, evaluator)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
            for (String alias : aliases) {
                if (name.equals(alias)) return cell.getColumnIndex();
            }
        }
        return fallback;
    }

    private int parseMarks(String value) {
        if (value.isBlank()) return 1;
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid marks: " + value);
        int marks = (int) Double.parseDouble(matcher.group());
        if (marks <= 0) throw new IllegalArgumentException("Marks must be greater than zero");
        return marks;
    }

    private String normalizeCorrectAnswer(
            String value,
            String optionA,
            String optionB,
            String optionC,
            String optionD
    ) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("^(OPTION\\s*)?A[\\).:-]?$")) return "A";
        if (normalized.matches("^(OPTION\\s*)?B[\\).:-]?$")) return "B";
        if (normalized.matches("^(OPTION\\s*)?C[\\).:-]?$")) return "C";
        if (normalized.matches("^(OPTION\\s*)?D[\\).:-]?$")) return "D";
        if (normalized.equals("1")) return "A";
        if (normalized.equals("2")) return "B";
        if (normalized.equals("3")) return "C";
        if (normalized.equals("4")) return "D";
        if (value.trim().equalsIgnoreCase(optionA.trim())) return "A";
        if (value.trim().equalsIgnoreCase(optionB.trim())) return "B";
        if (value.trim().equalsIgnoreCase(optionC.trim())) return "C";
        if (value.trim().equalsIgnoreCase(optionD.trim())) return "D";
        return normalized;
    }

    public record ImportResult(
            int imported,
            int failed,
            Long totalInExamPool,
            Integer questionsPerAttempt
    ) {}
}
