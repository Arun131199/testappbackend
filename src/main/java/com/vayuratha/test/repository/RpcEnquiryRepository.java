package com.vayuratha.test.repository;

import com.vayuratha.test.entity.RpcEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RpcEnquiryRepository extends JpaRepository<RpcEnquiry, Long> {

    @Query("SELECT COUNT(r) FROM RpcEnquiry r WHERE r.selected_slot = :slot AND r.paymentStatus = :status")
    long countBySlotAndPaymentStatus(@Param("slot") String slot, @Param("status") String status);

    @Query("SELECT r.selected_slot AS slot, COUNT(r) AS count FROM RpcEnquiry r WHERE r.paymentStatus = 'success' GROUP BY r.selected_slot")
    List<SlotCount> countBookedSeatsGroupedBySlot();

    interface SlotCount {
        String getSlot();
        Long getCount();
    }
}