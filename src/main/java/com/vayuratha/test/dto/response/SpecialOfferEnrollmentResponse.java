package com.vayuratha.test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpecialOfferEnrollmentResponse {

    private Long id;

    private String enrollmentId;

    private String name;

    private String mobile;

    private String email;

    private String offerName;

    private String additionalNotes;

    private String username;

    private String tempPassword;

    private String message;
}