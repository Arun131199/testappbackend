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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private final QuestionRepository questionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamRepository examRepository;

    public ImportResult importFromExcel(MultipartFile file, Long examId) throws IOException {
        if (examId != null && examRepository.findById(examId).isEmpty()) {
            throw new IllegalArgumentException("Exam not found: " + examId);
        }

        List<Question> toSave = new ArrayList<>();
        int errorCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String qText = getCell(row, 0);
                    String optA = getCell(row, 1);
                    String optB = getCell(row, 2);
                    String optC = getCell(row, 3);
                    String optD = getCell(row, 4);
                    String correct = getCell(row, 5).trim().toUpperCase();
                    String category = getCell(row, 6);
                    int marks = row.getCell(7) != null ? (int) row.getCell(7).getNumericCellValue() : 1;

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

        List<Question> saved = questionRepository.saveAll(toSave);

        if (examId != null) {
            for (Question question : saved) {
                if (!examQuestionRepository.existsByExamIdAndQuestionId(examId, question.getId())) {
                    examQuestionRepository.save(
                            ExamQuestion.builder().examId(examId).questionId(question.getId()).build()
                    );
                }
            }

            Exam exam = examRepository.findById(examId).orElseThrow();
            long total = examQuestionRepository.countByExamId(examId);
            exam.setQuestionCount((int) total);
            examRepository.save(exam);
        }

        return new ImportResult(saved.size(), errorCount);
    }

    private String getCell(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    public record ImportResult(int imported, int failed) {}
}