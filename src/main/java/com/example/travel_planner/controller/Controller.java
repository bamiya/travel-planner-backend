package com.example.travel_planner.controller;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.service.CommentService;
import com.example.travel_planner.service.LikeService;
import com.example.travel_planner.service.PasswordResetService;
import com.example.travel_planner.service.PlanService;
import com.example.travel_planner.service.UserService;
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

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody Users user) {
        return userService.register(user);
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

    @PostMapping("/getLikes")
    public ResponseEntity getLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return likeService.getLikes(token);
    }

    @PostMapping("/addLikes")
    public ResponseEntity addLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return likeService.addLikes(token, data);
    }

    @DeleteMapping("/removeLikes/{id}")
    public ResponseEntity removeLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String id){
        return likeService.removeLikes(token, id);
    }

    @GetMapping("/getLikeCount/{id}")
    public ResponseEntity getLikeCount(@PathVariable String id){
        return likeService.getLikeCount(id);
    }

    @PostMapping("/addComment")
    public ResponseEntity addComment(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> data){
        return commentService.addComment(token, data);
    }

    @GetMapping("/getComment")
    public ResponseEntity getComment(@RequestParam String id){
        return commentService.getComment(id);
    }
    @PostMapping("/getMyPage")
    public ResponseEntity getMyPage(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return commentService.getMyPage(token);
    }
    @PostMapping("/createPlan")
    public ResponseEntity createPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Map<String, String> plan) {
        return planService.createPlan(token, plan);
    }

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
    @GetMapping("/getUserPlanById/{id}")
    public ResponseEntity getUserPlanById(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String id){
        return planService.getUserPlanById(token, id);
    }

    @GetMapping("/getShareMyPlan")
    public ResponseEntity getShareMyPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return planService.getShareMyPlan(token);
    }
    @GetMapping("/getPlan")
    public ResponseEntity getPlan(){ return planService.getPlan();}

    @GetMapping("/getPlanWithPagination")
    public ResponseEntity getPlanWithPagination(@RequestParam String page, @RequestParam String size){
        return planService.getPlanWithPagination(page, size);}




    @GetMapping("/getPlansById/{id}")
    public ResponseEntity getPlansById(@PathVariable String id){
        return planService.getPlansById(id);
    }

}
