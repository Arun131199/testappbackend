package com.vayuratha.test.service;

import com.vayuratha.test.entity.RegistrationPayment;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.RegistrationPaymentRepository;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrationPaymentService {

    private final RegistrationPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WhatsAppService whatsAppService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final SecureRandom RANDOM = new SecureRandom();

    public RegistrationPayment createOrder(
            String fullName,
            String email,
            String mobile,
            long amountPaise,
            String orderId
    ) {

        if (userRepository.existsByEmail(email)
                || paymentRepository.existsByEmail(email)) {

            throw new IllegalArgumentException(
                    "This email is already registered"
            );
        }

        if (paymentRepository.existsByMobile(mobile)) {

            throw new IllegalArgumentException(
                    "This mobile number is already registered"
            );
        }

        RegistrationPayment payment = RegistrationPayment.builder()
                .fullName(fullName)
                .email(email)
                .mobile(mobile)
                .orderId(orderId)
                .amountPaise(amountPaise)
                .status("CREATED")
                .build();

        return paymentRepository.save(payment);
    }

    public void handlePaymentSuccess(
            String orderId,
            String paymentGatewayId
    ) {

        RegistrationPayment payment =
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown order: " + orderId
                                )
                        );


        // Already processed
        if ("SUCCESS".equals(payment.getStatus())) {
            return;
        }

        payment.setStatus("SUCCESS");
        payment.setPaymentGatewayId(paymentGatewayId);
        payment.setWebhookVerified(true);
        payment.setVerifiedAt(Instant.now());
        String generatedUserId = generateUserId();

        String defaultPassword = generateDefaultPassword();
        User user = User.builder()
                .userId(generatedUserId)
                .fullName(payment.getFullName())
                .email(payment.getEmail())
                .mobile(payment.getMobile())
                .passwordHash(
                        passwordEncoder.encode(defaultPassword)
                )
                .role(Role.USER)
                .build();

        userRepository.save(user);
        payment.setCreatedUserId(generatedUserId);
        paymentRepository.save(payment);
        try {

            whatsAppService.sendCredentials(
                    payment.getMobile(),
                    payment.getFullName(),
                    generatedUserId,
                    defaultPassword
            );

            System.out.println(
                    "======================================"
            );
            System.out.println(
                    "WHATSAPP CREDENTIALS SENT"
            );
            System.out.println(
                    "MOBILE   : " + payment.getMobile()
            );
            System.out.println(
                    "USERNAME : " + generatedUserId
            );
            System.out.println(
                    "======================================"
            );

        } catch (Exception e) {

            // WhatsApp failure should NOT undo user creation
            System.err.println(
                    "WhatsApp credential sending failed: "
                            + e.getMessage()
            );
        }
    }

    private String generateUserId() {

        Long nextVal = ((Number) entityManager
                .createNativeQuery(
                        "SELECT nextval('user_id_seq')"
                )
                .getSingleResult())
                .longValue();

        return String.format(
                "USR%03d",
                nextVal
        );
    }


    private String generateDefaultPassword() {

        int otp =
                100000 + RANDOM.nextInt(900000);

        return "Vayu@" + otp;
    }
}