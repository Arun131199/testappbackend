package com.vayuratha.test.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecialOfferEnrollmentRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String mobile;

    private String offerName;

    private String additionalNotes;

    // Payment details

    @NotBlank
    private String paymentStatus;

    @NotBlank
    private String paymentId;

    @NotBlank
    private String orderId;
}