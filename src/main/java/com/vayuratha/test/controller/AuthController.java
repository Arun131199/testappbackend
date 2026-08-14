package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.LoginRequest;
import com.vayuratha.test.dto.request.RegisterRequest;
import com.vayuratha.test.dto.respoonse.AuthResponse;
import com.vayuratha.test.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.cookie.secure:false}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return withAuthCookie(authService.registerUser(req));
    }

    @PostMapping("login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return withAuthCookie(authService.login(req));
    }

    private ResponseEntity<AuthResponse> withAuthCookie(AuthResponse response) {

        ResponseCookie accessTokenCookie =
                ResponseCookie.from(
                                "access_token",
                                response.getToken()
                        )
                        .httpOnly(true)
                        .secure(secureCookie)
                        .sameSite("None")
                        .path("/")
                        .maxAge(jwtExpirationMs / 1000)
                        .build();

        ResponseCookie userIdCookie =
                userCookie("userId", response.getUserId());

        ResponseCookie usernameCookie =
                userCookie("username", response.getFullName());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessTokenCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        userIdCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        usernameCookie.toString()
                )
                .body(response);
    }
    private ResponseCookie userCookie(String name, String value) {

        return ResponseCookie.from(
                        name,
                        URLEncoder.encode(value, StandardCharsets.UTF_8)
                )
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite("None")
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .build();
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie("access_token", true).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookie("userId", false).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookie("username", false).toString())
                .build();
    }

    private ResponseCookie expiredCookie(
            String name,
            boolean httpOnly
    ) {
        return ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
    }
}
