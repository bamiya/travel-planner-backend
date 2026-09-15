package com.example.travel_planner.repository;

import com.example.travel_planner.entity.Users;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.Optional;

@SpringBootTest
public class LikesRepositoryTests {
    @Autowired
    TourLikeRepository tourLikeRepository;
    @Autowired
    PlanLikeRepository planLikeRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    public void testClass(){
        System.out.println(tourLikeRepository.getClass().getName());
        System.out.println(planLikeRepository.getClass().getName());
    }

    @Transactional
    @Test
    public void getLikes(){
        Optional<Users> user = userRepository.findByEmail("test@test.com");
        System.out.println(user);
    }
}
