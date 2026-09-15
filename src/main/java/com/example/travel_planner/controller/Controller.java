package com.example.travel_planner.controller;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.config.Views;
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
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/kakaoLogin")
    public ResponseEntity kakaoLogin(@RequestParam String token) {
        return userService.getUserInfoKakao(token);
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody Map<String, String> data) {
        return userService.login(data);
    }

    @PostMapping("/checkEmail")
    public ResponseEntity checkEmail(@RequestBody Map<String, String> email) {
        return userService.checkEmail(email);
    }

    // 본인 계정 조회 - 연락처/생년월일/주소 등 전체 정보를 내려줘야 하므로 Owner 뷰
    @JsonView(Views.Owner.class)
    @GetMapping("/getUserInfo")
    public ResponseEntity getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return userService.getUserInfo(token);
    }
    @PostMapping("/getUserUpdatePw")
    public ResponseEntity getUserUpdatePw(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return userService.getUserUpdatePw(token, data);
    }

    @PostMapping("/getUserUpdate")
    public ResponseEntity getUserUpdate(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data) {
       return userService.getUserUpdate(token, data);
    }
    @DeleteMapping("/userDelete")
    public ResponseEntity userDelete(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return userService.userDelete(token);
    }

    // Map으로 받아 이메일/비밀번호/이름/연락처/생년월일/프로필사진만 서비스에서 골라 쓴다.
    // Users 엔티티를 그대로 바인딩하면 role="ADMIN" 같은 필드까지 클라이언트가 직접 지정할 수 있어 위험하다.
    @PostMapping("/register")
    public ResponseEntity register(@RequestBody Map<String, String> data) {
        return userService.register(data);
    }

    @PostMapping("/tokenAuth") // 그저 테스트
    public ResponseEntity tokenAuth(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if(jwtTokenProvider.validateAccessToken(token.split(" ")[1])){
            return new StatusCode(HttpStatus.OK, "인증 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    @PostMapping("/getTokenUsedRefreshToken")
    public ResponseEntity getTokenUsedRefreshToken(@RequestBody Map<String, String> data){
        return userService.getTokenUsedRefreshToken(data);
    }

    @PostMapping("/uploadFile")
    public ResponseEntity uploadFile(@RequestParam("file") MultipartFile multipartFile, @RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return userService.uploadFile(multipartFile, token);
    }

    @GetMapping(value="/image/view", produces= MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getImage(@RequestParam("value") String value) throws IOException {
        return userService.getImage(value);
    }

    // 비밀번호 찾기 절차: 이메일 확인 -> 인증코드 발송 -> 인증코드 검증(resetToken 발급) -> 비밀번호 변경
    @PostMapping("/sendResetCode")
    public ResponseEntity sendResetCode(@RequestBody Map<String, String> data) {
        return passwordResetService.sendResetCode(data.get("email"));
    }

    @PostMapping("/verifyResetCode")
    public ResponseEntity verifyResetCode(@RequestBody Map<String, String> data) {
        return passwordResetService.verifyResetCode(data.get("email"), data.get("code"));
    }

    @PostMapping("/passwordChange")
    public ResponseEntity passwordChange(@RequestBody Map<String, String> data) {
        return userService.passwordChange(data);
    }

    @JsonView(Views.Public.class)
    @PostMapping("/getLikes")
    public ResponseEntity getLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return likeService.getLikes(token);
    }

    @PostMapping("/addLikes")
    public ResponseEntity addLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return likeService.addLikes(token, data);
    }

    // type: "T"(관광지) 또는 "P"(플랜) - 좋아요 대상 id가 두 테이블에서 겹칠 수 있어 구분이 필요하다.
    @DeleteMapping("/removeLikes/{id}")
    public ResponseEntity removeLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String id, @RequestParam(defaultValue = "T") String type){
        return likeService.removeLikes(token, id, type);
    }

    @GetMapping("/getLikeCount/{id}")
    public ResponseEntity getLikeCount(@PathVariable String id){
        return likeService.getLikeCount(id);
    }

    @JsonView(Views.Public.class)
    @PostMapping("/addComment")
    public ResponseEntity addComment(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return commentService.addComment(token, data);
    }

    // type: "T"(관광지) 또는 "P"(플랜) - id가 두 테이블에서 겹칠 수 있어 구분이 필요하다.
    @JsonView(Views.Public.class)
    @GetMapping("/getComment")
    public ResponseEntity getComment(@RequestParam String id, @RequestParam(defaultValue = "T") String type){
        return commentService.getComment(id, type);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getMyComments")
    public ResponseEntity getMyComments(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return commentService.getMyComments(token);
    }
    @PostMapping("/createPlan")
    public ResponseEntity createPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> plan) {
        return planService.createPlan(token, plan);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getUserPlan")
    public ResponseEntity getUserPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return planService.getUserPlan(token);
    }

    @PutMapping("/updateSharePlan")
    public ResponseEntity updateSharePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return planService.updateSharePlan(token, data);
    }

    @PutMapping("updatePlan")
    public ResponseEntity updatePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return planService.updatePlan(token, data);
    }

    @DeleteMapping(value = "/deleteUserPlan/{id}")
    public ResponseEntity deleteUserPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String id){
        return planService.deleteUserPlan(token, id);
    }
    @JsonView(Views.Public.class)
    @GetMapping("/getUserPlanById/{id}")
    public ResponseEntity getUserPlanById(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String id){
        return planService.getUserPlanById(token, id);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getShareMyPlan")
    public ResponseEntity getShareMyPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return planService.getShareMyPlan(token);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/getPlan")
    public ResponseEntity getPlan(){ return planService.getPlan();}

    @JsonView(Views.Public.class)
    @GetMapping("/getPlanWithPagination")
    public ResponseEntity getPlanWithPagination(@RequestParam String page, @RequestParam String size){
        return planService.getPlanWithPagination(page, size);}

    @JsonView(Views.Public.class)
    @GetMapping("/getPlansById/{id}")
    public ResponseEntity getPlansById(@PathVariable String id){
        return planService.getPlansById(id);
    }

}
