package com.example.travel_planner.service;

import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.PlanComment;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.TargetType;
import com.example.travel_planner.entity.TourComment;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PlanCommentRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.example.travel_planner.repository.TourCommentRepository;
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

    @Transactional
    public ResponseEntity<?> addComment(Users user, Map<String, String> data) {
        String id = data.get("id");
        TargetType type = TargetType.valueOf(data.getOrDefault("type", "T"));

        Integer rating = null;
        if (data.get("rating") != null) {
            try {
                rating = Integer.valueOf(data.get("rating"));
            } catch (NumberFormatException e) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "별점은 숫자여야 합니다.").sendResponse();
            }
            if (rating < 1 || rating > 5) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "별점은 1~5 사이여야 합니다.").sendResponse();
            }
        }

        if (type == TargetType.P) {
            Optional<Plans> plan = planRepository.findById(Long.valueOf(id));
            if (plan.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
            }
            planCommentRepository.save(PlanComment.builder()
                    .user(user).plan(plan.get()).content(data.get("content")).rating(rating).date(LocalDate.now()).build());
        } else {
            tourCommentRepository.save(TourComment.builder()
                    .user(user).contentid(id).content(data.get("content")).rating(rating).date(LocalDate.now()).build());
        }
        return new StatusCode(HttpStatus.OK, "댓글 추가 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> deleteComment(Users user, String id, String type) {
        boolean isAdmin = user.getRole() == Users.Role.ADMIN;
        if (TargetType.valueOf(type) == TargetType.P) {
            Optional<PlanComment> comment = planCommentRepository.findById(Long.valueOf(id));
            if (comment.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.").sendResponse();
            }
            if (!isAdmin && !comment.get().getUser().getId().equals(user.getId())) {
                return new StatusCode(HttpStatus.FORBIDDEN, "본인 또는 관리자만 댓글을 삭제할 수 있습니다.").sendResponse();
            }
            planCommentRepository.deleteById(Long.valueOf(id));
        } else {
            Optional<TourComment> comment = tourCommentRepository.findById(Long.valueOf(id));
            if (comment.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.").sendResponse();
            }
            if (!isAdmin && !comment.get().getUser().getId().equals(user.getId())) {
                return new StatusCode(HttpStatus.FORBIDDEN, "본인 또는 관리자만 댓글을 삭제할 수 있습니다.").sendResponse();
            }
            tourCommentRepository.deleteById(Long.valueOf(id));
        }
        return new StatusCode(HttpStatus.OK, "댓글이 삭제되었습니다.").sendResponse();
    }

    public ResponseEntity<?> getComment(String id, String type, Users viewer) {
        if (TargetType.valueOf(type) == TargetType.P) {
            List<PlanComment> comments = planCommentRepository.findByPlanId(Long.valueOf(id));
            if (viewer != null) {
                comments.forEach(c -> c.setMine(c.getUser().getId().equals(viewer.getId())));
            }
            return new StatusCode(HttpStatus.OK, comments, "플랜댓글 조회성공").sendResponse();
        }
        List<TourComment> comments = tourCommentRepository.findByContentid(id);
        if (viewer != null) {
            comments.forEach(c -> c.setMine(c.getUser().getId().equals(viewer.getId())));
        }
        return new StatusCode(HttpStatus.OK, comments, "관광지댓글 조회성공").sendResponse();
    }

    public ResponseEntity<?> getMyComments(Users user) { // 내가 쓴 댓글 목록 (관광지+플랜 합쳐서)
        List<Object> comments = new ArrayList<>();
        comments.addAll(tourCommentRepository.findByUserOrderByDateDesc(user));
        comments.addAll(planCommentRepository.findByUserOrderByDateDesc(user));
        return new StatusCode(HttpStatus.OK, comments, "내 댓글 조회 성공").sendResponse();
    }
}
