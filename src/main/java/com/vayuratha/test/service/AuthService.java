package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.LoginRequest;
import com.vayuratha.test.dto.request.RegisterRequest;
import com.vayuratha.test.dto.respoonse.AuthResponse;
import com.vayuratha.test.entity.User;
import com.vayuratha.test.repository.UserRepository;
import com.vayuratha.test.roleEnum.Role;
import com.vayuratha.test.security.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String generatedUserId = generateUserId();

        User user = User.builder()
                .userId(generatedUserId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() == null ? Role.USER : request.getRole())
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUserId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getUserId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private String generateUserId() {
        Long nextVal = ((Number) entityManager
                .createNativeQuery("SELECT nextval('user_id_seq')")
                .getSingleResult()).longValue();
        return String.format("USR%03d", nextVal);   // USR001, USR002, USR010, USR100...
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getRole() != request.getRole()) {
            throw new IllegalArgumentException("The selected role does not match this account");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUserId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getUserId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
