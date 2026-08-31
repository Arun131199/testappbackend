package com.vayuratha.test.repository;

import com.vayuratha.test.entity.Exam;
import com.vayuratha.test.entity.RegistrationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationPaymentRepository extends JpaRepository<RegistrationPayment,Long> {
    Optional<RegistrationPayment> findByOrderId(String orderId);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
}
