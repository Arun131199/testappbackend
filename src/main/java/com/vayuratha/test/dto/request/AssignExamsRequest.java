package com.vayuratha.test.dto.request;

import java.util.List;

public record AssignExamsRequest(List<Long> examIds) {
}