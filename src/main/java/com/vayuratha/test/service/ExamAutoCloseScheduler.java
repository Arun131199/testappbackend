package com.vayuratha.test.service;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExamAutoCloseScheduler {

    private final ExamRepository examRepository;

    // Runs every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void autoCloseExpiredExams() {
        Instant now = Instant.now();
        List<Exam> liveExams = examRepository.findByStatus(Exam.ExamStatus.LIVE);

        for (Exam exam : liveExams) {
            boolean expired = exam.getEndTime() != null && now.isAfter(exam.getEndTime());
            if (expired) {
                exam.setStatus(Exam.ExamStatus.CLOSED);
                examRepository.save(exam);
            }
        }
    }
}
