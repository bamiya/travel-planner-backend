package com.example.travel_planner.service;

import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.PlanLike;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.TargetType;
import com.example.travel_planner.entity.TourLike;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PlanLikeRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.example.travel_planner.repository.TourLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LikeService {
    @Autowired
    private TourLikeRepository tourLikeRepository;
    @Autowired
    private PlanLikeRepository planLikeRepository;
    @Autowired
    private PlanRepository planRepository;

    public ResponseEntity getLikes(Users user) {
        // 프론트는 관광지 좋아요/플랜 좋아요를 한 배열로 받아 type("T"/"P")으로 걸러 쓴다.
        List<Object> likes = new ArrayList<>();
        likes.addAll(tourLikeRepository.findByUser(user));
        likes.addAll(planLikeRepository.findByUser(user));
        return new StatusCode(HttpStatus.OK, likes, "좋아요 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity addLikes(Users user, Map<String, String> data) {
        String id = data.get("id");
        TargetType type = TargetType.valueOf(data.getOrDefault("type", "T"));

        if (type == TargetType.P) {
            Optional<Plans> plan = planRepository.findById(Long.valueOf(id));
            if (plan.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
            }
            if (planLikeRepository.findByUserAndPlanId(user, plan.get().getId()).isEmpty()) {
                planLikeRepository.save(PlanLike.builder().user(user).plan(plan.get()).build());
            }
        } else {
            if (tourLikeRepository.findByUserAndContentid(user, id).isEmpty()) {
                tourLikeRepository.save(TourLike.builder().user(user).contentid(id).build());
            }
        }
        return new StatusCode(HttpStatus.OK, "좋아요 추가 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity removeLikes(Users user, String id, String type) {
        if (TargetType.valueOf(type) == TargetType.P) {
            planLikeRepository.findByUserAndPlanId(user, Long.valueOf(id)).ifPresent(planLikeRepository::delete);
        } else {
            tourLikeRepository.findByUserAndContentid(user, id).ifPresent(tourLikeRepository::delete);
        }
        return new StatusCode(HttpStatus.OK, "좋아요 삭제 성공").sendResponse();
    }

    public ResponseEntity getLikeCount(String id) {
        long cnt = tourLikeRepository.countByContentid(id);
        return new StatusCode(HttpStatus.OK, cnt, "좋아요 수 불러오기 완료").sendResponse();
    }
}
