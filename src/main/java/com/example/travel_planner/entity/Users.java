package com.example.travel_planner.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import javax.persistence.*;
import java.util.List;

@Entity //JPA로 관리되는 엔티티객체. 즉 테이블
@Table
@ToString
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor //빈생성자
public class Users {
    @Id
    private String email;
    @JsonIgnore
    @Column(length = 100, nullable = false)
    private String password;
    @Column(length = 50, nullable = false)
    private String name;
    @Column(length = 50)
    private String profileImg;
    @Column(length = 12, nullable = false)
    private String tel;
    @Column(nullable = false)
    private java.time.LocalDate birth;

//    @OneToMany(mappedBy = "email", orphanRemoval = true) // 좋아요 쪽 매핑 mappedBy는 필트 명이 아닌 변수 명으로 해야함. orphanRemoval은 삭제, 수정 시 관계되어있거 다 삭제
//    private List<Likes> likes;
//
//    @OneToMany(mappedBy = "email", orphanRemoval = true) // 댓글 쪽 매핑 mappedBy는 필트 명이 아닌 변수 명으로 해야함
//    private List<Comments> comments;
//
//    @OneToMany(mappedBy = "email", orphanRemoval = true) // 플랜 쪽 매핑 mappedBy는 필트 명이 아닌 변수 명으로 해야함
//    private List<Plans> plans;
}
