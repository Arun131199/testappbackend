package com.vayuratha.test.dto.request;

import java.util.List;

public record AssignUsersRequest(List<String> userIds) {
}