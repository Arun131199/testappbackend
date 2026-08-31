package com.vayuratha.test.repository;

import com.vayuratha.test.entity.RpcEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RpcEnquiryRepository extends JpaRepository<RpcEnquiry,Long> {
}
