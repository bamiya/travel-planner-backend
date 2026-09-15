package com.example.travel_planner.service;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.PlanComment;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.TourComment;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PlanCommentRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.example.travel_planner.repository.TourCommentRepository;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommentService {
    @Autowired
    private TourCommentRepository tourCommentRepository;
    @Autowired
    private PlanCommentRepository planCommentRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public ResponseEntity addComment(String token, Map<String, String> data) {
        String tokenFilter = token.split(" ")[1];
        if (!jwtTokenProvider.validateAccessToken(tokenFilter)) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
        Users user = userRepository.findByEmail(jwtTokenProvider.getUserEmailFromToken(tokenFilter)).orElseThrow();
        String id = data.get("id");
        String type = data.get("type");

        if ("P".equals(type)) {
            Optional<Plans> plan = planRepository.findById(Long.valueOf(id));
            if (plan.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
            }
            planCommentRepository.save(PlanComment.builder()
                    .user(user).plan(plan.get()).content(data.get("content")).date(LocalDate.now()).build());
        } else {
            tourCommentRepository.save(TourComment.builder()
                    .user(user).contentid(id).content(data.get("content")).date(LocalDate.now()).build());
        }
        return new StatusCode(HttpStatus.OK, "댓글 추가 성공").sendResponse();
    }

    public ResponseEntity getComment(String id, String type) {
        if ("P".equals(type)) {
            List<PlanComment> comments = planCommentRepository.findByPlanId(Long.valueOf(id));
            return new StatusCode(HttpStatus.OK, comments, "플랜댓글 조회성공").sendResponse();
        }
        List<TourComment> comments = tourCommentRepository.findByContentid(id);
        return new StatusCode(HttpStatus.OK, comments, "관광지댓글 조회성공").sendResponse();
    }

    public ResponseEntity getMyComments(String token) { // 내가 쓴 댓글 목록 (관광지+플랜 합쳐서)
        String tokenFilter = token.split(" ")[1];
        if (!jwtTokenProvider.validateAccessToken(tokenFilter)) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
        Users user = userRepository.findByEmail(jwtTokenProvider.getUserEmailFromToken(tokenFilter)).orElseThrow();
        List<Object> comments = new ArrayList<>();
        comments.addAll(tourCommentRepository.findByUserOrderByDateDesc(user));
        comments.addAll(planCommentRepository.findByUserOrderByDateDesc(user));
        return new StatusCode(HttpStatus.OK, comments, "내 댓글 조회 성공").sendResponse();
    }
}
