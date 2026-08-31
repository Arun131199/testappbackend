package com.vayuratha.test.dto.request;

import com.vayuratha.test.roleEnum.Role;
import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;

    private String email;

    private String password;

    private String mobile;

    private Role role;
}