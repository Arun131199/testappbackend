package com.vayuratha.test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProvisioningResponse {

    private String username;

    private String tempPassword;
}