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
    // 댓글 삭제(관리자용)가 이 값으로 대상을 찾는다 - @JsonView가 없으면 기본 뷰
    // 포함 설정에 따라 응답에서 조용히 빠질 수 있어(실제로 그랬다) 명시해둔다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.Public.class)
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

    // 1~5 별점 - 좋아요와 별개로, 댓글 작성 시 같이 남기는 평가. 기존 댓글엔 없을 수 있어
    // nullable로 두고(과거 데이터 마이그레이션 없이도 컬럼 추가 가능), 새 댓글은 서비스
    // 레이어에서 1~5 범위인지 검증한다.
    @JsonView(Views.Public.class)
    private Integer rating;

    @Column(nullable = false)
    @JsonView(Views.Public.class)
    private LocalDate date;

    // TourComment.mine 참고 - 로그인한 조회자 본인 댓글인지 서버가 계산해 내려준다.
    @Transient
    @Setter
    @Getter
    @JsonView(Views.Public.class)
    private boolean mine;

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
