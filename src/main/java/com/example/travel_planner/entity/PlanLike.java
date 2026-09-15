package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "plan_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "plan_id"}))
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 좋아요 목록은 항상 "내가 누른 것"만 조회하므로 누가 눌렀는지는 응답에 노출할 필요가 없다.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plans plan;

    @JsonProperty("id")
    @JsonView(Views.Public.class)
    public String getTargetId() {
        return plan != null ? String.valueOf(plan.getId()) : null;
    }

    @JsonProperty("type")
    @JsonView(Views.Public.class)
    public String getTargetType() {
        return "P";
    }
}
