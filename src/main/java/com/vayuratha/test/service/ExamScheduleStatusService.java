package com.vayuratha.test.service;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamScheduleStatusService {

    private final ExamRepository examRepository;

    @Scheduled(fixedDelayString = "${app.exam.schedule-status-check-ms:60000}")
    @Transactional
    public void updateScheduledExamStatuses() {
        Instant now = Instant.now();
        List<Exam> changedExams = examRepository.findByIsArchivedFalse().stream()
                .filter(exam -> updateStatus(exam, now))
                .toList();

        if (!changedExams.isEmpty()) {
            examRepository.saveAll(changedExams);
        }
    }

    private boolean updateStatus(Exam exam, Instant now) {
        if (exam.getEndTime() != null && !exam.getEndTime().isAfter(now)
                && exam.getStatus() != Exam.ExamStatus.CLOSED) {
            exam.setStatus(Exam.ExamStatus.CLOSED);
            return true;
        }

        if (exam.getStatus() == Exam.ExamStatus.DRAFT
                && exam.getStartTime() != null
                && !exam.getStartTime().isAfter(now)) {
            exam.setStatus(Exam.ExamStatus.LIVE);
            return true;
        }

        return false;
    }
}
