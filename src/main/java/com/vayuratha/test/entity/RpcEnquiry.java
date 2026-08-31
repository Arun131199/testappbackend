package com.vayuratha.test.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "rpc_enquiry")
public class RpcEnquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String selected_slot;

    @Column(nullable = false, updatable = true, unique = true)
    private String enquiryId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String mobile_number;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String address;

    private String additional_note;
}
