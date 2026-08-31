package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.CareerDevRequest;
import com.vayuratha.test.dto.response.CareerDevResponse;
import com.vayuratha.test.entity.CareerDevEntity;
import com.vayuratha.test.repository.CareerDevRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CareerDevService {

    private final CareerDevRepository repository;

    public CareerDevResponse createCareerDev(CareerDevRequest careerDevRequest) {
        String enquiryId = generateEnquiryId();

        CareerDevEntity careerDev = CareerDevEntity.builder()
                .enrollmentId(enquiryId)
                .name(careerDevRequest.getName())
                .email(careerDevRequest.getEmail())
                .address(careerDevRequest.getAddress())
                .education(careerDevRequest.getEducation())
                .hadRpc(careerDevRequest.getHadRpc())
                .mobile(careerDevRequest.getMobile())
                .additional_notes(careerDevRequest.getAdditional_notes())
                .build();

        CareerDevEntity savedEnquiry = repository.save(careerDev);

        return new CareerDevResponse(
                savedEnquiry.getId(),
                savedEnquiry.getEnrollmentId(),
                savedEnquiry.getName(),
                savedEnquiry.getEmail(),
                savedEnquiry.getMobile(),
                savedEnquiry.getEducation(),
                savedEnquiry.getHadRpc(),
                savedEnquiry.getAddress(),
                savedEnquiry.getAdditional_notes()
        );
    }

    private String generateEnquiryId() {
        return "RPC-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}