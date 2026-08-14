package com.vayuratha.test.service;

import com.vayuratha.test.dto.respoonse.AdminStatsResponse;
import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamAssignment;
import com.vayuratha.test.entity.ExamAttempt;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.ExamAssignmentRepository;
import com.vayuratha.test.repository.ExamAttemptRepository;
import com.vayuratha.test.repository.ExamRepository;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAdminService {

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final UserRepository userRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public Exam createExam(Exam exam) {
        exam.setStatus(Exam.ExamStatus.DRAFT);
        return examRepository.save(exam);
    }

    public Exam publishExam(Long examId, Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (!endTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException("End time must be in the future");
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.LIVE);
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        return examRepository.save(exam);
    }
    public Exam closeExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.CLOSED);
        return examRepository.save(exam);
    }

    public String assignToUsers(Long examId, List<String> userIds) {
        int count = 0;
        for (String userId : userIds) {
            if (!examAssignmentRepository.existsByExamIdAndUserId(examId, userId)) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(userId).build()
                );
                count++;
            }
        }
        return "Assigned to " + count + " new student(s)";
    }

    public String assignToAllStudents(Long examId) {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .toList();

        int count = 0;
        for (User student : students) {
            if (!examAssignmentRepository.existsByExamIdAndUserId(examId, student.getUserId())) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(student.getUserId()).build()
                );
                count++;
            }
        }
        return "Assigned to " + count + " new student(s) out of " + students.size() + " total";
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public List<Exam> getLiveExams() {
        return examRepository.findByStatus(Exam.ExamStatus.LIVE);
    }

    public AdminStatsResponse getStats() {
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .count();

        long activeExams = examRepository.findByStatus(Exam.ExamStatus.LIVE).size();

        List<ExamAttempt> allAttempts = examAttemptRepository.findAll();
        List<ExamAttempt> submitted = allAttempts.stream()
                .filter(a -> a.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS)
                .toList();

        long passed = submitted.stream()
                .filter(a -> a.getScore() != null && a.getTotalMarks() != null
                        && a.getScore() >= (a.getTotalMarks() * 0.5))
                .count();
        double overallPassPct = submitted.isEmpty() ? 0 : (passed * 100.0 / submitted.size());

        Map<String, List<ExamAttempt>> byCategory = submitted.stream()
                .collect(Collectors.groupingBy(ExamAttempt::getCategory));

        List<AdminStatsResponse.CategoryStat> categoryStats = byCategory.entrySet().stream()
                .map(e -> {
                    List<ExamAttempt> list = e.getValue();
                    long catPassed = list.stream()
                            .filter(a -> a.getScore() != null && a.getTotalMarks() != null
                                    && a.getScore() >= (a.getTotalMarks() * 0.5))
                            .count();
                    double pct = list.isEmpty() ? 0 : (catPassed * 100.0 / list.size());
                    return new AdminStatsResponse.CategoryStat(e.getKey(), list.size(), pct);
                }).toList();

        return new AdminStatsResponse(totalStudents, activeExams, overallPassPct, categoryStats);
    }
}
