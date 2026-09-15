package com.example.travel_planner.repository;

import com.example.travel_planner.entity.Comments;
import com.example.travel_planner.entity.Likes;
import com.example.travel_planner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comments, Integer> {
    @Query(value = "select * from comments where id = :id", nativeQuery = true)
    List<Comments> findById(String id);

    @Query(value = "select * from comments where email = :email order by date desc", nativeQuery = true)
    List<Comments> findByEmail(String email);
}
