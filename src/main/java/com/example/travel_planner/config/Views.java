package com.example.travel_planner.config;

// 응답 JSON에 어디까지 노출할지 구분하는 Jackson @JsonView 마커.
// Public: 다른 사용자에게도 보여도 되는 최소 정보(이름/프로필사진 등) - 플랜 작성자, 댓글 작성자 표시용
// Owner:  본인 계정 조회/수정 화면에서만 필요한 전체 정보(연락처/생년월일/주소 등)
public class Views {
    public interface Public {}
    public interface Owner extends Public {}
}
