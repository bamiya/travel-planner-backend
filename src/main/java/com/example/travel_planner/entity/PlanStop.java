package com.example.travel_planner.entity;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "plan_stops")
@ToString(exclude = "planDay")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private PlanDay planDay;

    @Column(length = 50, nullable = false)
    private String contentid; // TourAPI contentId

    @Column(length = 200)
    private String title;

    @Column(length = 255)
    private String addr1;

    @Column(length = 255)
    private String addr2;

    @Column(length = 10)
    private String zipcode;

    @Column(length = 20)
    private String tel;

    @Column(length = 500)
    private String firstimage;

    @Column(length = 500)
    private String firstimage2;

    private String mapx;

    private String mapy;

    @Column(nullable = false)
    private int orderIndex;
}
