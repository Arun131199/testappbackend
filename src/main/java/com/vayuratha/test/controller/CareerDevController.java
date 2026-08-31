package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.CareerDevRequest;
import com.vayuratha.test.dto.response.CareerDevResponse;
import com.vayuratha.test.service.CareerDevService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/career-dev")
@RequiredArgsConstructor
public class CareerDevController {
    private final CareerDevService careerDevService;

    @PostMapping("/enquiry")
    public ResponseEntity<CareerDevResponse> createEnquiry(
            @Valid @RequestBody CareerDevRequest request
            ){
        CareerDevResponse response=careerDevService.createCareerDev(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

}
