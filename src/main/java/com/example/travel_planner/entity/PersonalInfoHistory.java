package com.example.travel_planner.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

// 개인정보의 안전성 확보조치 기준(개인정보보호위원회 고시) 제8조 접속기록 보관 의무 대응용.
// 개인정보 자체(이름/연락처 등 실제 값)는 남기지 않고, 언제/누가/무슨 처리를 했는지만 남긴다
// (최소수집 원칙). email은 Users에 대한 FK가 아니라 그 시점의 스냅샷 문자열이다 - 탈퇴로 Users
// row가 삭제된 뒤에도 이 기록은 남아있어야 하기 때문이다.
@Entity
@Table(name = "personal_info_history")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonalInfoHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ActionType actionType;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @PrePersist
    protected void onCreate() {
        actionAt = LocalDateTime.now();
    }

    public enum ActionType { SIGN_UP, UPDATE_INFO, UPDATE_PASSWORD, WITHDRAW }
}
