package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "tour_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "contentid"}))
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 좋아요 목록은 항상 "내가 누른 것"만 조회하므로 누가 눌렀는지는 응답에 노출할 필요가 없다.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(length = 50, nullable = false)
    private String contentid;

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
