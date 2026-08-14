package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.RegisterRequest;
import com.vayuratha.test.dto.respoonse.AuthResponse;
import com.vayuratha.test.dto.respoonse.UserResponse;
import com.vayuratha.test.entity.Question;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.QuestionRepository;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import com.vayuratha.test.service.AuthService;
import com.vayuratha.test.service.QuestionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin")
public class AdminController {

    private final AuthService authService;
    private final QuestionImportService questionImportService;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;


    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() != Role.ADMIN)
                        .map(this::toUserResponse)
                        .toList()
        );
    }

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionRepository.findAll());
    }

    // Admin registers a student/user — this is the real "register user" flow
    @PostMapping("/users")
    public ResponseEntity<AuthResponse> createUser(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.registerUser(req));
    }

    // Admin bulk-imports questions via Excel
    @PostMapping(value = "/questions/import", consumes = "multipart/form-data")
    public ResponseEntity<QuestionImportService.ImportResult> importQuestions(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(questionImportService.importFromExcel(file));
    }
    private UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getUserId(), u.getFullName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
