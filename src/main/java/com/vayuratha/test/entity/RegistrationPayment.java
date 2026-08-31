package com.vayuratha.test.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "registration_payments")
@Builder
public  class RegistrationPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    @Column(nullable = false, unique = true)
    private String orderId;

    private String paymentGatewayId;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    @Builder.Default
    private String status="CREATED";

    @Column(nullable = false)
    @Builder.Default
    private Boolean webhookVerified=false;

    private String createdUserId;

    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    private Instant verifiedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}