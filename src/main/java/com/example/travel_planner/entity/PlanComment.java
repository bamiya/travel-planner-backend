package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "plan_comments")
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("email")
    @JsonView(Views.Public.class)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plans plan;

    @Column(columnDefinition = "longtext", nullable = false)
    @JsonView(Views.Public.class)
    private String content;

    @Column(nullable = false)
    @JsonView(Views.Public.class)
    private LocalDate date;

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
