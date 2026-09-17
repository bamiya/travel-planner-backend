package com.example.travel_planner.entity;

import com.example.travel_planner.config.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    // id/email은 계정 식별 정보라 다른 사용자에게 보이면 안 된다 (Owner 뷰만).
    // 댓글/플랜 작성자처럼 남에게 보여지는 자리엔 nickname/profileImg만 노출된다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.Owner.class)
    private Long id;

    @Column(length = 255, nullable = false, unique = true)
    @JsonView(Views.Owner.class)
    private String email;

    @JsonIgnore
    @Column(length = 100)
    private String password; // 소셜 전용 계정은 로컬 비밀번호가 없어 NULL 허용

    // 실명 - 계정 본인 확인용이라 다른 사용자에게 보이면 안 된다 (Owner 뷰만).
    // 댓글/플랜 작성자처럼 남에게 보여지는 자리엔 nickname을 쓴다.
    @Column(length = 50, nullable = false)
    @JsonView(Views.Owner.class)
    private String name;

    // 닉네임 - 댓글/플랜 작성자 등 다른 사용자에게 공개되는 표시 이름.
    // 기존 계정(카카오 로그인 등)에는 아직 없을 수 있어 컬럼 자체는 nullable로 두고,
    // 일반 회원가입 화면에서는 필수 입력으로 검증한다. 실제 응답 직렬화는 getDisplayNickname()이
    // 담당해서, nickname이 비어있는 기존 계정도 이름으로 대체 표시된다(응답이 빈 문자열로
    // 깨지지 않도록).
    @Column(length = 30, unique = true)
    @JsonIgnore
    private String nickname;

    @JsonProperty("nickname")
    @JsonView(Views.Public.class)
    public String getDisplayNickname() {
        return (nickname != null && !nickname.isBlank()) ? nickname : name;
    }

    @Column(length = 50)
    @JsonView(Views.Public.class)
    private String profileImg;

    // 아래부터는 본인 계정 조회/수정 화면 전용 - 다른 사용자에게 노출되면 안 되는 정보라 Owner 뷰에서만 직렬화된다.
    @Column(length = 20)
    @JsonView(Views.Owner.class)
    private String tel;

    @JsonView(Views.Owner.class)
    private java.time.LocalDate birth;

    @Column(length = 10)
    @JsonView(Views.Owner.class)
    private String zipcode;

    @Column(length = 255)
    @JsonView(Views.Owner.class)
    private String address1; // 기본주소(도로명/지번)

    @Column(length = 255)
    @JsonView(Views.Owner.class)
    private String address2; // 상세주소

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    @JsonView(Views.Owner.class)
    private Provider provider = Provider.LOCAL;

    @Column(length = 100)
    @JsonIgnore
    private String providerId; // 소셜 로그인 제공자가 주는 고유 회원 식별자 (내부용, 응답에 노출 안 함)

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    @JsonView(Views.Owner.class)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    @JsonView(Views.Owner.class)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @JsonView(Views.Owner.class)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Provider { LOCAL, KAKAO, NAVER }
    public enum Role { USER, ADMIN }
}
