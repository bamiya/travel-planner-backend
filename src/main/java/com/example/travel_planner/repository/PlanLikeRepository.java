package com.example.travel_planner.repository;

import com.example.travel_planner.entity.PlanLike;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanLikeRepository extends JpaRepository<PlanLike, Long> {
    List<PlanLike> findByUser(Users user);

    Optional<PlanLike> findByUserAndPlanId(Users user, Long planId);

    long countByPlanId(Long planId);

    void deleteByUser(Users user);

    void deleteByPlanIn(List<Plans> plans);
}
