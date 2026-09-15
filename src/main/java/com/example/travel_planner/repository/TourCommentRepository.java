package com.example.travel_planner.repository;

import com.example.travel_planner.entity.TourComment;
import com.example.travel_planner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourCommentRepository extends JpaRepository<TourComment, Long> {
    List<TourComment> findByContentid(String contentid);

    List<TourComment> findByUserOrderByDateDesc(Users user);

    void deleteByUser(Users user);
}
