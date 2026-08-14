package com.vayuratha.test.dto.respoonse;

import com.vayuratha.test.roleEnum.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    // Public application identifier (for example, USR001), not the database primary key.
    private String userId;
    private String fullName;
    private String email;
    private Role role;
}
