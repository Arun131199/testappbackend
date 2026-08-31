package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.SpecialOfferEnrollmentRequest;
import com.vayuratha.test.dto.response.SpecialOfferEnrollmentResponse;
import com.vayuratha.test.service.SpecialOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/special-offers")
@RequiredArgsConstructor
public class SpecialOfferController {

    private final SpecialOfferService specialOfferService;

    @PostMapping("/enroll")
    public ResponseEntity<SpecialOfferEnrollmentResponse> enroll(
            @Valid @RequestBody SpecialOfferEnrollmentRequest request
    ) {

        SpecialOfferEnrollmentResponse response =
                specialOfferService.enroll(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}