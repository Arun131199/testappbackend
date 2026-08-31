package com.vayuratha.test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerDevResponse {
    private Long id;
    private String enrollmentId;
    private String name;
    private String email;
    private String mobile;
    private String education;
    private Boolean hadRpc;
    private String address;
    private String additional_notes;
}