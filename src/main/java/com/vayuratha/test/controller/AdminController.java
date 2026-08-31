package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.RegisterRequest;
import com.vayuratha.test.dto.response.AuthResponse;
import com.vayuratha.test.dto.response.UserResponse;
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

    @PostMapping("/users")
    public ResponseEntity<AuthResponse> createUser(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.registerUser(req));
    }

    // Bulk-import questions via Excel.Pass examId to link every imported question directly to that exam.
    // Omit examId to import into the general question bank only.
    @PostMapping(value = "/questions/import", consumes = "multipart/form-data")
    public ResponseEntity<QuestionImportService.ImportResult> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examId", required = false) Long examId) throws IOException {
        return ResponseEntity.ok(questionImportService.importFromExcel(file, examId));
    }

    private UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getUserId(), u.getFullName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}