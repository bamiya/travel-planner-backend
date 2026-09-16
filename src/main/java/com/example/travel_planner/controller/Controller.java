package com.example.travel_planner.controller;

import com.example.travel_planner.config.CurrentUser;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.config.Views;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.service.CommentService;
import com.example.travel_planner.service.LikeService;
import com.example.travel_planner.service.PasswordResetService;
import com.example.travel_planner.service.PlanService;
import com.example.travel_planner.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/")
public class Controller {

    @Autowired
    private UserService userService;
    @Autowired
    private PlanService planService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code) {
        return userService.getUserInfoKakao(code);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
        return userService.login(data);
    }

    @PostMapping("/checkEmail")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> email) {
        return userService.checkEmail(email);
    }

    // 본인 계정 조회 - 연락처/생년월일/주소 등 전체 정보를 내려줘야 하므로 Owner 뷰
    @JsonView(Views.Owner.class)
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@CurrentUser Users user) {
        return userService.getUserInfo(user);
    }
    @PostMapping("/getUserUpdatePw")
    public ResponseEntity<?> getUserUpdatePw(@CurrentUser Users user, @RequestBody Map<String, String> data){
        return userService.getUserUpdatePw(user, data);
    }

    @PostMapping("/getUserUpdate")
    public ResponseEntity<?> getUserUpdate(@CurrentUser Users user, @RequestBody Map<String, String> data) {
       return userService.getUserUpdate(user, data);
    }
    @DeleteMapping("/userDelete")
    public ResponseEntity<?> userDelete(@CurrentUser Users user){
        return userService.userDelete(user);
    }

    // Map으로 받아 이메일/비밀번호/이름/연락처/생년월일/프로필사진만 서비스에서 골라 쓴다.
    // Users 엔티티를 그대로 바인딩하면 role="ADMIN" 같은 필드까지 클라이언트가 직접 지정할 수 있어 위험하다.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> data) {
        return userService.register(data);
    }

    // 리프레시 토큰은 요청 본문이 아니라 httpOnly 쿠키로 들어온다 (서비스 레이어에서 읽는다).
    @PostMapping("/getTokenUsedRefreshToken")
    public ResponseEntity<?> getTokenUsedRefreshToken(){
        return userService.getTokenUsedRefreshToken();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        return userService.logout();
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile multipartFile, @CurrentUser Users user){
        return userService.uploadFile(multipartFile, user);
    }

    @GetMapping(value="/image/view", produces= MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getImage(@RequestParam("value") String value) throws IOException {
        return userService.getImage(value);
    }

    // 비밀번호 찾기 절차: 이메일 확인 -> 인증코드 발송 -> 인증코드 검증(resetToken 발급) -> 비밀번호 변경
    @PostMapping("/sendResetCode")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> data) {
        return passwordResetService.sendResetCode(data.get("email"));
    }

    @PostMapping("/verifyResetCode")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> data) {
        return passwordResetService.verifyResetCode(data.get("email"), data.get("code"));
    }

    @PostMapping("/passwordChange")
    public ResponseEntity<?> passwordChange(@RequestBody Map<String, String> data) {
        return userService.passwordChange(data);
    }

    @JsonView(Views.Public.class)
    @PostMapping("/getLikes")
    public ResponseEntity<?> getLikes(@CurrentUser Users user){
        return likeService.getLikes(user);
    }

    @PostMapping("/addLikes")
    public ResponseEntity<?> addLikes(@CurrentUser Users user, @RequestBody Map<String, String> data){
        return likeService.addLikes(user, data);
    }

    // type: "T"(관광지) 또는 "P"(플랜) - 좋아요 대상 id가 두 테이블에서 겹칠 수 있어 구분이 필요하다.
    @DeleteMapping("/removeLikes/{id}")
    public ResponseEntity<?> removeLikes(@CurrentUser Users user, @PathVariable String id, @RequestParam(defaultValue = "T") String type){
        return likeService.removeLikes(user, id, type);
    }

    @GetMapping("/getLikeCount/{id}")
    public ResponseEntity<?> getLikeCount(@PathVariable String id){
        return likeService.getLikeCount(id);
    }

    @JsonView(Views.Public.class)
    @PostMapping("/addComment")
    public ResponseEntity<?> addComment(@CurrentUser Users user, @RequestBody Map<String, String> data){
        return commentService.addComment(user, data);
    }

    // type: "T"(관광지) 또는 "P"(플랜) - id가 두 테이블에서 겹칠 수 있어 구분이 필요하다.
    @JsonView(Views.Public.class)
    @GetMapping("/getComment")
    public ResponseEntity<?> getComment(@RequestParam String id, @RequestParam(defaultValue = "T") String type){
        return commentService.getComment(id, type);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getMyComments")
    public ResponseEntity<?> getMyComments(@CurrentUser Users user){
        return commentService.getMyComments(user);
    }
    @PostMapping("/createPlan")
    public ResponseEntity<?> createPlan(@CurrentUser Users user, @RequestBody Map<String, String> plan) {
        return planService.createPlan(user, plan);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getUserPlan")
    public ResponseEntity<?> getUserPlan(@CurrentUser Users user) {
        return planService.getUserPlan(user);
    }

    @PutMapping("/updateSharePlan")
    public ResponseEntity<?> updateSharePlan(@CurrentUser Users user, @RequestBody Map<String, String> data){
        return planService.updateSharePlan(user, data);
    }

    @PutMapping("updatePlan")
    public ResponseEntity<?> updatePlan(@CurrentUser Users user, @RequestBody Map<String, String> data){
        return planService.updatePlan(user, data);
    }

    @DeleteMapping(value = "/deleteUserPlan/{id}")
    public ResponseEntity<?> deleteUserPlan(@CurrentUser Users user, @PathVariable String id){
        return planService.deleteUserPlan(user, id);
    }
    @JsonView(Views.Public.class)
    @GetMapping("/getUserPlanById/{id}")
    public ResponseEntity<?> getUserPlanById(@CurrentUser Users user, @PathVariable String id){
        return planService.getUserPlanById(user, id);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getShareMyPlan")
    public ResponseEntity<?> getShareMyPlan(@CurrentUser Users user) {
        return planService.getShareMyPlan(user);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getPlan")
    public ResponseEntity<?> getPlan(){ return planService.getPlan();}

    @JsonView(Views.Public.class)
    @GetMapping("/getPlanWithPagination")
    public ResponseEntity<?> getPlanWithPagination(@RequestParam String page, @RequestParam String size){
        return planService.getPlanWithPagination(page, size);}

    @JsonView(Views.Public.class)
    @GetMapping("/getPlansById/{id}")
    public ResponseEntity<?> getPlansById(@PathVariable String id){
        return planService.getPlansById(id);
    }

    // 토큰은 유효하지만(서명/만료 통과) 그 안의 이메일에 해당하는 회원이 DB에 없는 경우
    // (다른 기기에서 탈퇴했거나 관리자가 삭제한 경우 등) 서비스 곳곳의 .orElseThrow()가
    // NoSuchElementException을 던진다. 이걸 그냥 500으로 흘려보내는 대신, "다시 로그인해주세요"
    // 의미의 401로 통일해서 응답한다.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleUserNotFound() {
        return new StatusCode(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다. 다시 로그인해주세요.").sendResponse();
    }

}
