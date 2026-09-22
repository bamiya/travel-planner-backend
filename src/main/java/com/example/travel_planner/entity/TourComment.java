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
    // 댓글 삭제(관리자용)가 이 값으로 대상을 찾으니 @JsonView를 명시해서 응답에 꼭 포함시킨다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.Public.class)
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

    // 1~5 별점 - 좋아요와 별개로, 댓글 작성 시 같이 남기는 평가. 기존 댓글엔 없을 수 있어
    // nullable로 두고, 새 댓글은 서비스 레이어에서 1~5 범위인지 검증한다.
    @JsonView(Views.Public.class)
    private Integer rating;

    @Column(nullable = false)
    @JsonView(Views.Public.class)
    private LocalDate date;

    // 로그인한 조회자 본인이 쓴 댓글인지 - 셀프 삭제 버튼 노출 여부를 프론트가 판단하는 데
    // 쓴다. Users의 id/email은 더 이상 응답에 노출되지 않으므로(닉네임만 노출) 프론트가
    // 직접 비교할 수 없어, Plans.mine과 같은 방식으로 서버가 대신 계산해서 내려준다.
    @Transient
    @Setter
    @Getter
    @JsonView(Views.Public.class)
    private boolean mine;

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
