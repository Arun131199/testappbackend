package com.vayuratha.test.controller;

import com.vayuratha.test.dto.request.RpcEnquiryRequest;
import com.vayuratha.test.dto.response.RpcEnquiryResponse;
import com.vayuratha.test.service.RpcEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rpc-enquiry")
@RequiredArgsConstructor
public class RpcController {
    private final RpcEnquiryService rpcEnquiryService;

    @PostMapping
    public ResponseEntity<RpcEnquiryResponse> createEnquiry(
            @Valid @RequestBody RpcEnquiryRequest request
    ) {

        RpcEnquiryResponse response =rpcEnquiryService.createEnquiry(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
