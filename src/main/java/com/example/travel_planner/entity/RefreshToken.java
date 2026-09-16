package com.example.travel_planner.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

// 발급된 리프레시 토큰의 서버측 기록. 토큰 문자열 자체(JWT)는 서명만 맞으면 서버가
// 상태 없이도 검증할 수 있지만, 그것만으로는 로그아웃/탈취 시 무효화할 방법이 없다.
// 여기 기록된 것만 "아직 유효한" 토큰으로 취급하고, 재발급마다 폐기 후 새로 발급한다
// (rotation) - 탈취된 토큰이 재사용되면 이미 폐기되어 거부된다.
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 토큰 원문이 아니라 해시만 저장한다 - DB가 유출돼도 토큰을 그대로 재사용할 수 없게.
    @Column(length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
