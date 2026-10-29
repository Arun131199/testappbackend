package com.vayuratha.test.dto.request;

import lombok.Data;

@Data
public class ExamUpdateRequest {
    private String title;
    private String category;
    private Integer questionCount;
    private Integer durationMinutes;
    // IST format: yyyy-MM-dd'T'HH:mm:ss  (publish endpoint-la use panra same format)
    private String startTime;
    private String endTime;
    // DRAFT / LIVE / CLOSED
    private String status;
    private Boolean isDefaultForNewUsers;
}