package com.example.travel_planner.service;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.LikeRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class PlanService {
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public ResponseEntity createPlan(String token, Map<String, String> plan) {
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> user = userRepository.findById(getUserEmailFromToken);
            try{
                Plans plans = Plans.builder()
                        .email(user.get())
                        .title(plan.get("title"))
                        .plan(plan.get("plan"))
                        .date(plan.get("date"))
                        .type(Integer.parseInt(plan.get("type")))
                        .build();
                planRepository.save(plans);
                return new StatusCode(HttpStatus.OK, "플랜 생성이 완료되었습니다!").sendResponse();
            }catch (Exception e){
                return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
            }
        }else {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity getUserPlan(String token){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            List<Plans> resultPlans = planRepository.getPlansByEmail(getUserEmailFromToken);
            Collections.reverse(resultPlans);
            return new StatusCode(HttpStatus.OK, resultPlans, "유저 플랜 조회 성공").sendResponse();
        } else {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "이미 만료된 유저임").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity updateSharePlan(String token, Map<String, String> data){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);

            Optional<Users> user = userRepository.findById(getUserEmailFromToken);
            Plans resultPlan = planRepository.getPlansByEmailAndId(getUserEmailFromToken, data.get("id"));

            if(resultPlan == null){
                return new StatusCode(HttpStatus.BAD_REQUEST, "알 수 없는 오류").sendResponse();
            }else {
                Plans plans = Plans.builder()
                        .id(resultPlan.getId())
                        .email(user.get())
                        .title(resultPlan.getTitle())
                        .plan(resultPlan.getPlan())
                        .date(resultPlan.getDate())
                        .type(resultPlan.getType() == 0 ? 1 : 0)
                        .build();
                planRepository.save(plans);
                return new StatusCode(HttpStatus.OK, "유저 비/공개 변경 성공").sendResponse();
            }
        } else {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity deleteUserPlan(String token, String id){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);

            Optional<Users> user = userRepository.findById(getUserEmailFromToken);
            Plans plan = planRepository.getPlansByEmailAndId(getUserEmailFromToken, id);

            planRepository.deleteByIdx(plan.getId());
            planRepository.deleteCommentByIdxAndType(plan.getId(), "P");
            likeRepository.deleteByIdAndType(plan.getId(), "P");    //Type P => Plan
            return new StatusCode(HttpStatus.OK, "유저 플랜 삭제 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity getUserPlanById(String token, String id){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            Plans plan = planRepository.getPlansByEmailAndId(jwtTokenProvider.getUserEmailFromToken(tokenFilter), id);
            if(plan == null){
                return new StatusCode(HttpStatus.NOT_FOUND, "유저 단일 플랜 조회 못함").sendResponse();
            }
            return new StatusCode(HttpStatus.OK, plan, "유저 단일 플랜 조회 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }
    public ResponseEntity getShareMyPlan(String token){ //공유된플랜조회
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            List<Plans> PlanType = planRepository.getSharedPlanType(jwtTokenProvider.getUserEmailFromToken(tokenFilter));
            Collections.reverse(PlanType);
            return new StatusCode(HttpStatus.OK, PlanType, "공유된플랜조회 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity getPlan(){
        List<Plans> plans =  planRepository.getPlans();
        Collections.reverse(plans);
        System.out.println("가나요:" + plans);
        for(int i = 0; i < plans.size(); i++){
            int cnt = likeRepository.selectLikeCount(plans.get(i).getId());
            plans.get(i).setLikeCount(cnt);
        }
        return new StatusCode(HttpStatus.OK, plans, "공유된플랜보기 조회성공").sendResponse();
    }

    public ResponseEntity getPlanWithPagination(String page, String size){
        Pageable pageRequest = PageRequest.of(Integer.parseInt(page), Integer.parseInt(size));
        List<Plans> plans = (List<Plans>) planRepository.getPlans(pageRequest);
        Collections.reverse(plans);
        for(int i = 0; i < plans.size(); i++){
            int cnt = likeRepository.selectLikeCount(plans.get(i).getId());
            plans.get(i).setLikeCount(cnt);
        }Long totalSize = planRepository.sharePlanCount();
        List returnData = new ArrayList();
        returnData.add(totalSize);
        returnData.add(plans);
        return new StatusCode(HttpStatus.OK, returnData, "페이지네이션").sendResponse();
    }

    public ResponseEntity getPlansById(String id){
        Plans plan =  planRepository.getPlansById(id);
        int cnt = likeRepository.selectLikeCount(Integer.parseInt(id));
        plan.setLikeCount(cnt);
        return new StatusCode(HttpStatus.OK, plan, "단일 플랜 조회 성공").sendResponse();
    }

    public ResponseEntity updatePlan(String token, Map<String, String> data){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            Optional<Users> user = userRepository.findById(jwtTokenProvider.getUserEmailFromToken(tokenFilter));
            Plans resultPlan = planRepository.getPlansById(data.get("id"));
            Plans plans = Plans.builder()
                    .id(resultPlan.getId())
                    .email(user.get())
                    .title(data.get("title"))
                    .plan(data.get("plan"))
                    .date(data.get("date"))
                    .type(resultPlan.getType())
                    .build();
            planRepository.save(plans);
            return new StatusCode(HttpStatus.OK, "업데이트 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }
}

