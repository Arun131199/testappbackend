package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.SpecialOfferEnrollmentRequest;
import com.vayuratha.test.dto.response.SpecialOfferEnrollmentResponse;
import com.vayuratha.test.dto.response.UserProvisioningResponse;
import com.vayuratha.test.entity.SpecialOfferEnrollment;
import com.vayuratha.test.repository.SpecialOfferEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpecialOfferService {

    private final SpecialOfferEnrollmentRepository repository;

    private final UserProvisioningService userProvisioningService;

    @Transactional
    public SpecialOfferEnrollmentResponse enroll(
            SpecialOfferEnrollmentRequest request
    ) {

        if (!"success".equalsIgnoreCase(request.getPaymentStatus())) {
            throw new IllegalArgumentException("Payment is not successful");
        }
        if (request.getPaymentId() == null || request.getPaymentId().isBlank()) {
            throw new IllegalArgumentException("Payment ID is required");
        }

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (repository.existsByPaymentId(request.getPaymentId())) {
            throw new IllegalArgumentException("This payment has already been processed");
        }

        if (repository.existsByOrderId(request.getOrderId())) {
            throw new IllegalArgumentException("This order has already been processed");
        }

        String enrollmentId = generateEnrollmentId();
        SpecialOfferEnrollment enrollment = SpecialOfferEnrollment.builder()
                        .enrollmentId(enrollmentId)
                        .name(request.getName())
                        .email(request.getEmail())
                        .mobile(request.getMobile())
                        .offerName(request.getOfferName())
                        .additionalNotes(request.getAdditionalNotes())
                        .paymentStatus(request.getPaymentStatus())
                        .paymentId(request.getPaymentId())
                        .orderId(request.getOrderId())
                        .build();

        SpecialOfferEnrollment savedEnrollment =repository.save(enrollment);
        UserProvisioningResponse user =userProvisioningService.createUserIfNotExists(
                        request.getName(),
                        request.getEmail(),
                        request.getMobile()
        );

        return new SpecialOfferEnrollmentResponse(
                savedEnrollment.getId(),
                savedEnrollment.getEnrollmentId(),
                savedEnrollment.getName(),
                savedEnrollment.getMobile(),
                savedEnrollment.getEmail(),
                savedEnrollment.getOfferName(),
                savedEnrollment.getAdditionalNotes(),
                user.getUsername(),
                user.getTempPassword(),
                "Special offer enrollment successful"
        );
    }

    private String generateEnrollmentId() {
        return "SO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
    }
}