package com.vayuratha.test.service;

import com.vayuratha.test.dto.request.RpcEnquiryRequest;
import com.vayuratha.test.dto.response.RpcEnquiryResponse;
import com.vayuratha.test.entity.RpcEnquiry;
import com.vayuratha.test.repository.RpcEnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RpcEnquiryService {

    private static final int SLOT_CAPACITY = 10;
    private static final String STATUS_SUCCESS = "success";

    private final RpcEnquiryRepository rpcEnquiryRepository;
    private final SendMail sendMail;
    private final WhatsAppService whatsAppService;
    private final RpcSheetService rpcSheetService;

    public RpcEnquiryResponse createEnquiry(RpcEnquiryRequest request) {
        long alreadyBooked = rpcEnquiryRepository.countBySlotAndPaymentStatus(
                request.getSelected_slot(), STATUS_SUCCESS
        );

        if (alreadyBooked >= SLOT_CAPACITY) {
            throw new IllegalStateException("This RPC slot is already full");
        }

        String enquiryId = generateEnquiryId();
        RpcEnquiry enquiry = RpcEnquiry.builder()
                .enquiryId(enquiryId)
                .name(request.getName())
                .email(request.getEmail())
                .mobile_number(request.getMobile_number())
                .qualification(request.getQualification())
                .address(request.getAddress())
                .additional_note(request.getAdditional_note())
                .selected_slot(request.getSelected_slot())
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .paymentStatus(STATUS_SUCCESS)
                .build();

        RpcEnquiry savedEnquiry = rpcEnquiryRepository.save(enquiry);

        int slotNumber = extractSlotNumber(savedEnquiry.getSelected_slot());
        LocalDate slotDate = calculateSlotDate(savedEnquiry.getSelected_slot());
        String formattedDate = slotDate != null
                ? slotDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "";

        if (slotDate != null) {
            sendMail.sendRpcBookingConfirmation(
                    savedEnquiry.getEmail(),
                    savedEnquiry.getName(),
                    slotNumber,
                    slotDate
            );
            whatsAppService.sendRpcBookingConfirmation(
                    savedEnquiry.getMobile_number(),
                    savedEnquiry.getName(),
                    slotNumber,
                    slotDate
            );
        }

        rpcSheetService.appendBooking(
                savedEnquiry.getEnquiryId(),
                savedEnquiry.getName(),
                savedEnquiry.getEmail(),
                savedEnquiry.getMobile_number(),
                savedEnquiry.getQualification(),
                savedEnquiry.getAddress(),
                savedEnquiry.getSelected_slot(),
                formattedDate,
                savedEnquiry.getRazorpayPaymentId(),
                savedEnquiry.getRazorpayOrderId()
        );

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

    public Map<String, Long> getBookedSeatsBySlot() {
        Map<String, Long> result = new HashMap<>();
        for (RpcEnquiryRepository.SlotCount row : rpcEnquiryRepository.countBookedSeatsGroupedBySlot()) {
            result.put(row.getSlot(), row.getCount());
        }
        return result;
    }

    // slot id format from frontend: "<year>-<month>-<thursdayOccurrenceInMonth>"
    // e.g. "2026-3-2" = 2nd Thursday of March 2026
    private LocalDate calculateSlotDate(String slotId) {
        try {
            String[] parts = slotId.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int thursdayOccurrence = Integer.parseInt(parts[2]);

            LocalDate date = LocalDate.of(year, month, 1);
            int thursdayCount = 0;

            while (date.getMonthValue() == month) {
                if (date.getDayOfWeek() == java.time.DayOfWeek.THURSDAY) {
                    thursdayCount++;
                    if (thursdayCount == thursdayOccurrence) {
                        return date;
                    }
                }
                date = date.plusDays(1);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Could not parse slot date from id: " + slotId);
            return null;
        }
    }

    private int extractSlotNumber(String slotId) {
        try {
            return Integer.parseInt(slotId.split("-")[2]);
        } catch (Exception e) {
            return 0;
        }
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