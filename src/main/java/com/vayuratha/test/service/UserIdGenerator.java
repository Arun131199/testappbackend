package com.vayuratha.test.service;

import com.vayuratha.test.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class UserIdGenerator {

    private static final int MAX_ATTEMPTS = 50;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public UserIdGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String next() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long nextVal = ((Number) entityManager
                    .createNativeQuery("SELECT nextval('ecom.user_id_seq')")
                    .getSingleResult())
                    .longValue();

            String userId = String.format("USR%03d", nextVal);

            if (!userRepository.existsByUserId(userId)) {
                return userId;
            }
        }

        throw new IllegalStateException("Could not generate a unique user id");
    }
}