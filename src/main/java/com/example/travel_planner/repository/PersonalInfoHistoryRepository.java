package com.example.travel_planner.repository;

import com.example.travel_planner.entity.PersonalInfoHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalInfoHistoryRepository extends JpaRepository<PersonalInfoHistory, Long> {
}
