package com.vayuratha.test.dto.request;

import lombok.Data;

@Data
public class ShareResultRequest {
    private String imageBase64;
    private String examTitle;
    private Integer score;
    private Integer totalMarks;
    private Double percentage;
}