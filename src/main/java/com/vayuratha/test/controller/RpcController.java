package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.RpcEnquiryRequest;
import com.vayuratha.test.dto.response.RpcEnquiryResponse;
import com.vayuratha.test.service.RpcEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rpc-enquiry")
@RequiredArgsConstructor
public class RpcController {
    private final RpcEnquiryService rpcEnquiryService;

    @PostMapping
    public ResponseEntity<?> createEnquiry(
            @Valid @RequestBody RpcEnquiryRequest request
    ) {
        try {
            RpcEnquiryResponse response = rpcEnquiryService.createEnquiry(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/available-seats")
    public ResponseEntity<Map<String, Long>> getBookedSeats() {
        return ResponseEntity.ok(rpcEnquiryService.getBookedSeatsBySlot());
    }
}