package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.RpcEnquiryRequest;
import com.vayuratha.test.dto.response.RpcEnquiryResponse;
import com.vayuratha.test.entity.RpcEnquiry;
import com.vayuratha.test.repository.RpcEnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RpcEnquiryService {

    private final RpcEnquiryRepository rpcEnquiryRepository;

    public RpcEnquiryResponse createEnquiry(RpcEnquiryRequest request) {
        String enquiryId=generateEnquiryId();
        RpcEnquiry enquiry = RpcEnquiry.builder()
                .enquiryId(enquiryId)
                .name(request.getName())
                .email(request.getEmail())
                .mobile_number(request.getMobile_number())
                .qualification(request.getQualification())
                .address(request.getAddress())
                .additional_note(request.getAdditional_note())
                .selected_slot(request.getSelected_slot())
                .build();

        RpcEnquiry savedEnquiry = rpcEnquiryRepository.save(enquiry);
        return new RpcEnquiryResponse(
                savedEnquiry.getId(),
                savedEnquiry.getEnquiryId(),
                savedEnquiry.getName(),
                savedEnquiry.getEmail(),
                savedEnquiry.getMobile_number(),
                savedEnquiry.getQualification(),
                savedEnquiry.getAddress(),
                savedEnquiry.getAdditional_note(),
                savedEnquiry.getSelected_slot()
        );
    }

    private String generateEnquiryId() {

        return "RPC-" +
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd")
                ) +
                "-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();
    }
}
