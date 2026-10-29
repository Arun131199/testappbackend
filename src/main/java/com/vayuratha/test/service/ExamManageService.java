package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.ExamUpdateRequest;
import com.vayuratha.test.dto.response.ExamAssigneeResponse;
import com.vayuratha.test.dto.response.UserExamAssignmentResponse;
import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.ExamAssignment;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.ExamAssignmentRepository;
import com.vayuratha.test.repository.ExamQuestionRepository;
import com.vayuratha.test.repository.ExamRepository;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamManageService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;

    // ===================== EXAM EDIT =====================

    public Exam getExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    @Transactional
    public Exam updateExam(Long examId, ExamUpdateRequest req) {
        Exam exam = getExam(examId);

        if (req.getTitle() != null) {
            String title = req.getTitle().trim();
            if (title.isEmpty()) {
                throw badRequest("Title cannot be empty");
            }
            exam.setTitle(title);
        }

        if (req.getCategory() != null) {
            String category = req.getCategory().trim();
            if (category.isEmpty()) {
                throw badRequest("Category cannot be empty");
            }
            exam.setCategory(category);
        }

        if (req.getDurationMinutes() != null) {
            if (req.getDurationMinutes() <= 0) {
                throw badRequest("Duration must be greater than zero");
            }
            exam.setDurationMinutes(req.getDurationMinutes());
        }

        if (req.getQuestionCount() != null) {
            int questionCount = req.getQuestionCount();
            if (questionCount <= 0) {
                throw badRequest("Question count must be greater than zero");
            }
            long linked = examQuestionRepository.countByExamId(examId);
            if (linked > 0 && questionCount > linked) {
                throw badRequest("This exam has only " + linked + " question(s) linked. "
                        + "Add more questions first or reduce the question count.");
            }
            exam.setQuestionCount(questionCount);
        }

        boolean scheduleTouched = req.getStartTime() != null
                || req.getEndTime() != null
                || req.getStatus() != null;

        Instant start = exam.getStartTime();
        Instant end = exam.getEndTime();

        if (req.getStartTime() != null) {
            start = parseIst(req.getStartTime(), "startTime");
        }
        if (req.getEndTime() != null) {
            end = parseIst(req.getEndTime(), "endTime");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            throw badRequest("End time must be after start time");
        }

        Exam.ExamStatus status = exam.getStatus();
        if (req.getStatus() != null) {
            try {
                status = Exam.ExamStatus.valueOf(req.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw badRequest("Status must be one of DRAFT, LIVE, CLOSED");
            }
        }

        if (scheduleTouched && status == Exam.ExamStatus.LIVE
                && end != null && !end.isAfter(Instant.now())) {
            throw badRequest("A LIVE exam must have an end time in the future");
        }

        exam.setStartTime(start);
        exam.setEndTime(end);
        exam.setStatus(status);

        if (req.getIsDefaultForNewUsers() != null) {
            exam.setIsDefaultForNewUsers(req.getIsDefaultForNewUsers());
        }

        return examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public List<ExamAssigneeResponse> getAssignees(Long examId) {
        getExam(examId);

        List<ExamAssignment> assignments = examAssignmentRepository.findByExamId(examId);
        Set<String> userIds = assignments.stream()
                .map(ExamAssignment::getUserId)
                .collect(Collectors.toSet());

        Map<String, User> users = userRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return assignments.stream()
                .sorted(Comparator.comparing(ExamAssignment::getAssignedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(a -> {
                    User user = users.get(a.getUserId());
                    return new ExamAssigneeResponse(
                            a.getUserId(),
                            user != null ? user.getFullName() : "Unknown",
                            user != null ? user.getEmail() : "",
                            user != null ? user.getMobile() : "",
                            Boolean.TRUE.equals(a.getCompleted()),
                            a.getAttemptId(),
                            a.getAssignedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> syncUsersForExam(Long examId, List<String> userIds) {
        getExam(examId);

        if (userIds == null) {
            throw badRequest("userIds is required");
        }

        Set<String> target = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, User> users = userRepository.findByUserIdIn(target).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<String> unknown = target.stream().filter(id -> !users.containsKey(id)).toList();
        if (!unknown.isEmpty()) {
            throw badRequest("Unknown user(s): " + String.join(", ", unknown));
        }

        List<String> notStudents = users.values().stream()
                .filter(u -> u.getRole() != Role.USER)
                .map(User::getUserId)
                .toList();
        if (!notStudents.isEmpty()) {
            throw badRequest("Only student accounts can be assigned: " + String.join(", ", notStudents));
        }

        List<ExamAssignment> current = examAssignmentRepository.findByExamId(examId);
        Set<String> currentIds = current.stream()
                .map(ExamAssignment::getUserId)
                .collect(Collectors.toSet());

        int added = 0;
        for (String userId : target) {
            if (!currentIds.contains(userId)) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(userId).build()
                );
                added++;
            }
        }

        int removed = 0;
        int skipped = 0;
        for (ExamAssignment assignment : current) {
            if (target.contains(assignment.getUserId())) {
                continue;
            }
            if (hasStarted(assignment)) {
                skipped++;
            } else {
                examAssignmentRepository.delete(assignment);
                removed++;
            }
        }

        return summary(added, removed, skipped, examAssignmentRepository.findByExamId(examId).size());
    }

    @Transactional
    public String unassign(Long examId, String userId) {
        ExamAssignment assignment = examAssignmentRepository.findByExamIdAndUserId(examId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "This exam is not assigned to that user"));

        if (hasStarted(assignment)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot remove: the student has already started or completed this exam");
        }

        examAssignmentRepository.delete(assignment);
        return "Unassigned " + userId + " from exam " + examId;
    }

    @Transactional(readOnly = true)
    public List<UserExamAssignmentResponse> getExamsForUser(String userId) {
        findStudent(userId);

        Map<Long, ExamAssignment> byExam = examAssignmentRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ExamAssignment::getExamId, Function.identity(), (a, b) -> a));

        return examRepository.findAll().stream()
                .sorted(Comparator.comparing(Exam::getId).reversed())
                .map(exam -> {
                    ExamAssignment a = byExam.get(exam.getId());
                    return new UserExamAssignmentResponse(
                            exam.getId(),
                            exam.getTitle(),
                            exam.getCategory(),
                            exam.getStatus(),
                            exam.getQuestionCount(),
                            exam.getDurationMinutes(),
                            a != null,
                            a != null && Boolean.TRUE.equals(a.getCompleted()),
                            a != null ? a.getAttemptId() : null,
                            a != null ? a.getAssignedAt() : null
                    );
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> syncExamsForUser(String userId, List<Long> examIds) {
        findStudent(userId);

        if (examIds == null) {
            throw badRequest("examIds is required");
        }

        Set<Long> target = new LinkedHashSet<>(examIds);
        target.remove(null);

        Set<Long> existingExamIds = examRepository.findAllById(target).stream()
                .map(Exam::getId)
                .collect(Collectors.toSet());

        List<String> unknown = target.stream()
                .filter(id -> !existingExamIds.contains(id))
                .map(String::valueOf)
                .toList();
        if (!unknown.isEmpty()) {
            throw badRequest("Unknown exam id(s): " + String.join(", ", unknown));
        }

        List<ExamAssignment> current = examAssignmentRepository.findByUserId(userId);
        Set<Long> currentExamIds = current.stream()
                .map(ExamAssignment::getExamId)
                .collect(Collectors.toSet());

        int added = 0;
        for (Long examId : target) {
            if (!currentExamIds.contains(examId)) {
                examAssignmentRepository.save(
                        ExamAssignment.builder().examId(examId).userId(userId).build()
                );
                added++;
            }
        }

        int removed = 0;
        int skipped = 0;
        for (ExamAssignment assignment : current) {
            if (target.contains(assignment.getExamId())) {
                continue;
            }
            if (hasStarted(assignment)) {
                skipped++;
            } else {
                examAssignmentRepository.delete(assignment);
                removed++;
            }
        }

        return summary(added, removed, skipped, examAssignmentRepository.findByUserId(userId).size());
    }

    //helpers
    private User findStudent(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.USER) {
            throw badRequest("Only student accounts can have exam assignments");
        }
        return user;
    }

    private boolean hasStarted(ExamAssignment assignment) {
        return Boolean.TRUE.equals(assignment.getCompleted()) || assignment.getAttemptId() != null;
    }

    private Instant parseIst(String value, String field) {
        try {
            LocalDateTime local = LocalDateTime.parse(value.trim(), FORMATTER);
            return ZonedDateTime.of(local, IST_ZONE).toInstant();
        } catch (DateTimeParseException ex) {
            throw badRequest(field + " must be in format yyyy-MM-dd'T'HH:mm:ss (IST)");
        }
    }

    private Map<String, Object> summary(int added, int removed, int skipped, int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", added);
        result.put("removed", removed);
        result.put("skippedAlreadyStarted", skipped);
        result.put("totalAssigned", total);
        return result;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}