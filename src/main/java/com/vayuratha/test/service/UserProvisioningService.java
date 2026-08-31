package com.vayuratha.test.service;

import com.vayuratha.test.dto.response.UserProvisioningResponse;
import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamAssignment;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.ExamAssignmentRepository;
import com.vayuratha.test.repository.ExamRepository;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WhatsAppService whatsAppService;
    private final SendMail emailService;

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public UserProvisioningResponse createUserIfNotExists(
            String fullName,
            String email,
            String mobile
    ) {
        var existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            return regenerateCredentials(
                    existingByEmail.get(),
                    fullName
            );
        }
        var existingByMobile = userRepository.findByMobile(mobile);

        if (existingByMobile.isPresent()) {
            return regenerateCredentials(
                    existingByMobile.get(),
                    fullName
            );
        }
        String userId = generateUserId();
        String temporaryPassword = generateDefaultPassword();

        User user = User.builder()
                .userId(userId)
                .fullName(fullName)
                .email(email)
                .mobile(mobile)
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        assignDefaultExamsToUser(savedUser.getUserId());
        whatsAppService.sendCredentials(
                savedUser.getMobile(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                temporaryPassword
        );
        emailService.sendCredentials(
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                temporaryPassword
        );
        return new UserProvisioningResponse(
                savedUser.getEmail(),
                temporaryPassword
        );
    }

    private void assignDefaultExamsToUser(String userId) {
        List<Exam> defaultExams = examRepository.findByStatusAndIsDefaultForNewUsers(Exam.ExamStatus.LIVE, true);
        for (Exam exam : defaultExams) {
            boolean alreadyAssigned = examAssignmentRepository.existsByExamIdAndUserId(exam.getId(), userId);
            if (!alreadyAssigned) {
                ExamAssignment assignment = ExamAssignment.builder()
                        .examId(exam.getId())
                        .userId(userId)
                        .completed(false)
                        .attemptId(null)
                        .assignedAt(Instant.now())
                        .build();
                examAssignmentRepository.save(assignment);
                System.out.println("AUTO EXAM ASSIGNED");
                System.out.println("USER : " + userId);
                System.out.println("EXAM : " + exam.getId());
                System.out.println("TITLE: " + exam.getTitle());
            }
        }
    }

    private UserProvisioningResponse regenerateCredentials(
            User existingUser,
            String fullName
    ) {
        String temporaryPassword = generateDefaultPassword();
        existingUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        if (fullName != null && !fullName.isBlank()) {
            existingUser.setFullName(fullName);
        }
        User updatedUser = userRepository.save(existingUser);
        whatsAppService.sendCredentials(
                updatedUser.getMobile(),
                updatedUser.getFullName(),
                updatedUser.getEmail(),
                temporaryPassword
        );
        emailService.sendCredentials(
                updatedUser.getEmail(),
                updatedUser.getFullName(),
                updatedUser.getEmail(),
                temporaryPassword
        );
        return new UserProvisioningResponse(
                updatedUser.getEmail(),
                temporaryPassword
        );
    }

    private String generateUserId() {
        Long nextId = userRepository.getNextUserId();
        return String.format("USR%03d", nextId);
    }
    private String generateDefaultPassword() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return "Vayu@" + otp;
    }
}