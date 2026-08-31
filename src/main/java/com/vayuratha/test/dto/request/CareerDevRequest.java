package com.vayuratha.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CareerDevRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String email;

    @NotBlank
    private String mobile;

    @NotBlank
    private String education;

    @NotNull(message = "hadRpc must be true or false")
    private Boolean hadRpc;

    @NotBlank
    private String address;

    private String additional_notes;
}