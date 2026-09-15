package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tour_comments")
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourComment {
    // 프론트가 "id"를 댓글 자신의 PK가 아니라 "댓글이 달린 대상의 id"라는 의미로 써서
    // (예: like.filter(e => e.id === tour.contentid)), PK는 idx로 이름을 분리한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("email") // 프론트가 작성자를 el.email.* 로 읽던 기존 계약을 그대로 유지
    @JsonView(Views.Public.class)
    private Users user;

    @Column(length = 50, nullable = false)
    @JsonView(Views.Public.class)
    private String contentid; // TourAPI contentId

    @Column(columnDefinition = "longtext", nullable = false)
    @JsonView(Views.Public.class)
    private String content;

    @Column(nullable = false)
    @JsonView(Views.Public.class)
    private LocalDate date;

    // 프론트는 "관광지 댓글"/"플랜 댓글"을 id + type("T"/"P")로 구분해 쓴다 (myComment 페이지 등)
    @JsonProperty("id")
    @JsonView(Views.Public.class)
    public String getTargetId() {
        return contentid;
    }

    @JsonProperty("type")
    @JsonView(Views.Public.class)
    public String getTargetType() {
        return "T";
    }
}
