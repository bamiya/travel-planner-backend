package com.example.travel_planner.repository;

import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plans, Long> {
    List<Plans> findByUserOrderByIdDesc(Users user);

    Optional<Plans> findByUserAndId(Users user, Long id);

    List<Plans> findBySharedTrueOrderByIdDesc();

    List<Plans> findByUserAndSharedTrueOrderByIdDesc(Users user);

    Page<Plans> findBySharedTrue(Pageable pageable);

    long countBySharedTrue();

    long countByUserAndSharedTrue(Users user);
}
