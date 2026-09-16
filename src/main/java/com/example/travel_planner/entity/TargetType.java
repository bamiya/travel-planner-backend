package com.example.travel_planner.entity;

// 좋아요/댓글이 관광지(T)를 향한 건지 플랜(P)을 향한 건지 구분한다.
// 프론트-백엔드 API 계약(쿼리 파라미터 값)이 이미 "T"/"P"라서 그대로 상수명으로 쓴다.
public enum TargetType {
    T, // 관광지(Tour)
    P  // 플랜(Plan)
}
