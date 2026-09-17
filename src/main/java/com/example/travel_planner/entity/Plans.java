package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "plans")
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Plans {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.Public.class)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("email") // 프론트가 작성자를 el.email.* 로 읽던 기존 계약을 그대로 유지
    @JsonView(Views.Public.class)
    private Users user;

    @Column(length = 50, nullable = false)
    @JsonView(Views.Public.class)
    private String title;

    @JsonIgnore
    private LocalDate startDate;

    @JsonIgnore
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    @JsonIgnore
    private boolean shared = false; // 기존 type(0/1) 대체 - 공개(공유) 여부

    // Plans가 소유하는 하위 엔티티라 이 방향 cascade는 안전하다 (플랜 삭제 시 일자/장소도 같이 삭제되어야 함)
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("dayNumber ASC")
    @JsonIgnore
    private List<PlanDay> days = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;

    @Setter
    @Getter
    @Transient
    @JsonView(Views.Public.class)
    private int likeCount;

    // 작성자 email/id를 더 이상 응답에 안 내려주기 때문에(개인정보), 프론트가
    // "내 플랜인지"를 직접 비교할 방법이 없다 - 서버가 미리 계산해서 내려준다.
    @Setter
    @Getter
    @Transient
    @JsonView(Views.Public.class)
    private boolean mine;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- 프론트엔드 응답 호환용 파생 필드 ----
    // (day/stop 정규화 테이블을 프론트가 예전부터 써온 JSON 문자열 하나로 다시 합쳐 내려준다)

    @JsonProperty("date")
    @JsonView(Views.Public.class)
    public String getDateRangeString() {
        return startDate + "~" + endDate;
    }

    @JsonProperty("type")
    @JsonView(Views.Public.class)
    public int getTypeInt() {
        return shared ? 1 : 0;
    }

    @JsonProperty("plan")
    @JsonView(Views.Public.class)
    public String getPlanJson() {
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            for (PlanDay day : days) {
                Map<String, Object> dayMap = new LinkedHashMap<>();
                dayMap.put("day", day.getDayNumber());
                List<Map<String, Object>> list = new ArrayList<>();
                for (PlanStop stop : day.getStops()) {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("addr1", stop.getAddr1());
                    s.put("addr2", stop.getAddr2());
                    s.put("contentid", stop.getContentid());
                    s.put("firstimage", stop.getFirstimage());
                    s.put("firstimage2", stop.getFirstimage2());
                    s.put("mapx", stop.getMapx());
                    s.put("mapy", stop.getMapy());
                    s.put("tel", stop.getTel());
                    s.put("title", stop.getTitle());
                    s.put("zipcode", stop.getZipcode());
                    list.add(s);
                }
                dayMap.put("list", list);
                out.add(dayMap);
            }
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            return "[]";
        }
    }
}
