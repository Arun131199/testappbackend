package com.vayuratha.test.repository;

import com.vayuratha.test.entity.CareerDevEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerDevRepository extends JpaRepository<CareerDevEntity,Long> {
}
