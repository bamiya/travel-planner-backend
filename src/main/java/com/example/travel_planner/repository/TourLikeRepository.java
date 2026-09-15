package com.example.travel_planner.repository;

import com.example.travel_planner.entity.TourLike;
import com.example.travel_planner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourLikeRepository extends JpaRepository<TourLike, Long> {
    List<TourLike> findByUser(Users user);

    Optional<TourLike> findByUserAndContentid(Users user, String contentid);

    long countByContentid(String contentid);

    void deleteByUser(Users user);
}
