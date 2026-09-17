package com.example.travel_planner.repository;

import com.example.travel_planner.entity.PlanComment;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanCommentRepository extends JpaRepository<PlanComment, Long> {
    List<PlanComment> findByPlanId(Long planId);

    List<PlanComment> findByUserOrderByDateDesc(Users user);

    void deleteByUser(Users user);

    void deleteByPlanIn(List<Plans> plans);

    long countByUser(Users user);
}
