package com.example.travel_planner.entity;

import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan_days")
@ToString(exclude = "plan")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plans plan;

    @Column(nullable = false)
    private int dayNumber;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("orderIndex ASC")
    private List<PlanStop> stops = new ArrayList<>();
}
