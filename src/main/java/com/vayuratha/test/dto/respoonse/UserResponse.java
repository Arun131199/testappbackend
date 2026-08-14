package com.vayuratha.test.dto.respoonse;


import com.vayuratha.test.roleEnum.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String userId;
    private String fullName;
    private String email;
    private Role role;
    private Instant createdAt;
}
