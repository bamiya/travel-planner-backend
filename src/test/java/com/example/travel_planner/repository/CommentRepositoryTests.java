package com.example.travel_planner.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CommentRepositoryTests {

    @Autowired
    TourCommentRepository tourCommentRepository;

    @Autowired
    PlanCommentRepository planCommentRepository;

    @Test
    public void testClass(){
        System.out.println(tourCommentRepository.getClass().getName());
        System.out.println(planCommentRepository.getClass().getName());
    }
}
