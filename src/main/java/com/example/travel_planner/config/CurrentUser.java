package com.example.travel_planner.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 컨트롤러 메서드 파라미터에 붙이면 Authorization 헤더의 액세스 토큰을 검증하고
// 그 토큰 주인인 Users를 바로 주입해준다 (CurrentUserArgumentResolver가 처리).
// 예: public ResponseEntity getUserInfo(@CurrentUser Users user) { ... }
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
}
