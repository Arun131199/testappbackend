package com.vayuratha.test.dto.respoonse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalStudents;
    private long activeExams;
    private double overallPassPercentage;
    private List<CategoryStat> categoryStats;

    @Data
    @AllArgsConstructor
    public static class CategoryStat {
        private String category;
        private long totalAttempts;
        private double passPercentage;
    }
}
