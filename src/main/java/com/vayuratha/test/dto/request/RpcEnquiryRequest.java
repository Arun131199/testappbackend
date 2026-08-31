package com.vayuratha.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RpcEnquiryRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String mobile_number;
    @NotBlank
    private String qualification;
    @NotBlank
    private String address;

    private String additional_note;
    @NotBlank
    private String name;
    @NotBlank
    private String selected_slot;
}
