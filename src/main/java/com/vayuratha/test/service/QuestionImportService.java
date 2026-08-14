package com.vayuratha.test.service;

import com.vayuratha.test.entity.Question;
import com.vayuratha.test.repository.QuestionRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class QuestionImportService {
    @Autowired
    private final QuestionRepository questionRepository;

    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        List<Question> toSave=new ArrayList<>();
        int errorCount=0;
        try(Workbook workbook=new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i=1;i<=sheet.getLastRowNum();i++){
                Row row=sheet.getRow(i);
                if(row==null){
                    continue;
                }

                try{
                    String qText=getCell(row,0);
                    String optA=getCell(row,1);
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

                }catch (Exception e){
                    errorCount++;
                }
            }
        }
        questionRepository.saveAll(toSave);
        return new ImportResult(toSave.size(), errorCount);
    }

    private String getCell(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    public record ImportResult(int imported, int failed) {}

}
