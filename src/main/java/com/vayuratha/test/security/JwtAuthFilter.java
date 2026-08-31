package com.vayuratha.test.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@AllArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("========== JWT DEBUG ==========");
        System.out.println("REQUEST METHOD: " + request.getMethod());
        System.out.println("REQUEST URI: " + request.getRequestURI());

        String token = extractTokenFromHeader(request);

        if (token != null) {
            System.out.println("TOKEN SOURCE: Authorization Header");
        }

        if (token == null) {
            token = extractTokenFromCookie(request);

            if (token != null) {
                System.out.println("TOKEN SOURCE: Cookie");
            }
        }

        System.out.println("TOKEN EXISTS: " + (token != null));

        if (token == null) {
            System.out.println("JWT RESULT: No token found");
            System.out.println("==============================");

            filterChain.doFilter(request, response);
            return;
        }

        boolean validToken = jwtUtil.isValid(token);

        System.out.println("TOKEN VALID: " + validToken);

        if (!validToken) {
            System.out.println("JWT RESULT: Invalid token");
            System.out.println("==============================");

            filterChain.doFilter(request, response);
            return;
        }

        String role = jwtUtil.extractRole(token);
        String publicUserId = jwtUtil.extractPublicUserId(token);

        System.out.println("JWT ROLE: " + role);
        System.out.println("JWT PUBLIC USER ID: " + publicUserId);

        if (publicUserId == null || publicUserId.isBlank()) {

            System.out.println(
                    "JWT RESULT: Public user ID missing"
            );

            System.out.println("==============================");

            filterChain.doFilter(request, response);
            return;
        }

        if (role == null || role.isBlank()) {

            System.out.println(
                    "JWT RESULT: Role missing"
            );

            System.out.println("==============================");

            filterChain.doFilter(request, response);
            return;
        }

        String authorityName = "ROLE_" + role;

        var authority =
                new SimpleGrantedAuthority(authorityName);

        System.out.println(
                "SPRING AUTHORITY: "
                        + authority.getAuthority()
        );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        publicUserId,
                        null,
                        List.of(authority)
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        System.out.println(
                "AUTHENTICATION SET: "
                        + SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        System.out.println(
                "AUTHENTICATED USER: "
                        + authentication.getPrincipal()
        );

        System.out.println(
                "AUTHORITY: "
                        + authentication.getAuthorities()
        );

        System.out.println("JWT RESULT: SUCCESS");
        System.out.println("==============================");
        filterChain.doFilter(request, response);
    }
    private String extractTokenFromHeader(
            HttpServletRequest request
    ) {

        String header =
                request.getHeader("Authorization");

        if (header != null &&
                header.startsWith("Bearer ")) {

            return header.substring(7);
        }

        return null;
    }

    private String extractTokenFromCookie(
            HttpServletRequest request
    ) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if ("access_token".equals(
                    cookie.getName()
            )) {

                return cookie.getValue();
            }
        }

        return null;
    }
}