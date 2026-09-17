package com.example.travel_planner.repository;

import com.example.travel_planner.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
