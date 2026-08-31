package com.vayuratha.test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RpcEnquiryResponse {
    private Long id;
    private String enquiryId;
    private String name;
    private String email;
    private String mobile_number;
    private String qualification;
    private String address;
    private String additional_note;
    private String selected_slot;
}
