package com.vayuratha.test.repository;

import com.vayuratha.test.entity.SpecialOfferEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialOfferEnrollmentRepository
        extends JpaRepository<SpecialOfferEnrollment, Long> {

    Optional<SpecialOfferEnrollment> findByEmail(String email);

    Optional<SpecialOfferEnrollment> findByMobile(String mobile);

    Optional<SpecialOfferEnrollment> findByPaymentId(String paymentId);

    Optional<SpecialOfferEnrollment> findByOrderId(String orderId);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    boolean existsByPaymentId(String paymentId);

    boolean existsByOrderId(String orderId);
}