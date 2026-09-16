package com.example.travel_planner.service;

import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.PlanDay;
import com.example.travel_planner.entity.PlanStop;
import com.example.travel_planner.entity.Plans;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PlanCommentRepository;
import com.example.travel_planner.repository.PlanLikeRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class PlanService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private PlanLikeRepository planLikeRepository;
    @Autowired
    private PlanCommentRepository planCommentRepository;

    // ---- 프론트가 보내는 plan JSON( [{day, list:[...]}] )을 파싱하기 위한 최소 형태 ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IncomingDay {
        public int day;
        public List<IncomingStop> list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IncomingStop {
        public String addr1, addr2, contentid, firstimage, firstimage2, mapx, mapy, tel, title, zipcode;
    }

    private List<PlanDay> parseDays(String planJson) throws Exception {
        List<IncomingDay> incoming = MAPPER.readValue(planJson, MAPPER.getTypeFactory().constructCollectionType(List.class, IncomingDay.class));
        List<PlanDay> days = new ArrayList<>();
        for (IncomingDay d : incoming) {
            List<PlanStop> stops = new ArrayList<>();
            PlanDay day = PlanDay.builder().dayNumber(d.day).stops(stops).build();
            if (d.list != null) {
                int order = 0;
                for (IncomingStop s : d.list) {
                    stops.add(PlanStop.builder()
                            .planDay(day)
                            .contentid(s.contentid)
                            .title(s.title)
                            .addr1(s.addr1)
                            .addr2(s.addr2)
                            .zipcode(s.zipcode)
                            .tel(s.tel)
                            .firstimage(s.firstimage)
                            .firstimage2(s.firstimage2)
                            .mapx(s.mapx)
                            .mapy(s.mapy)
                            .orderIndex(order++)
                            .build());
                }
            }
            days.add(day);
        }
        return days;
    }

    private LocalDate[] parseDateRange(String dateRange) {
        String[] parts = dateRange.split("~");
        return new LocalDate[]{ LocalDate.parse(parts[0]), LocalDate.parse(parts[1]) };
    }

    @Transactional
    public ResponseEntity createPlan(Users user, Map<String, String> data) {
        try {
            LocalDate[] range = parseDateRange(data.get("date"));
            List<PlanDay> days = parseDays(data.get("plan"));

            Plans plan = Plans.builder()
                    .user(user)
                    .title(data.get("title"))
                    .startDate(range[0])
                    .endDate(range[1])
                    .shared(false)
                    .days(days)
                    .build();
            days.forEach(d -> setDayParent(d, plan));
            planRepository.save(plan);
            return new StatusCode(HttpStatus.OK, "플랜 생성이 완료되었습니다!").sendResponse();
        } catch (Exception e) {
            e.printStackTrace();
            return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
        }
    }

    private void setDayParent(PlanDay day, Plans plan) {
        // PlanDay.builder()는 plan 필드를 안 받으므로(Plans가 아직 없을 때 만들어짐) 여기서 역참조를 채워준다.
        day.setPlan(plan);
    }

    @Transactional
    public ResponseEntity getUserPlan(Users user) {
        List<Plans> resultPlans = planRepository.findByUserOrderByIdDesc(user);
        return new StatusCode(HttpStatus.OK, resultPlans, "유저 플랜 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity updateSharePlan(Users user, Map<String, String> data) {
        Optional<Plans> resultPlan = planRepository.findByUserAndId(user, Long.valueOf(data.get("id")));

        if (resultPlan.isEmpty()) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "알 수 없는 오류").sendResponse();
        }
        Plans plan = resultPlan.get();
        plan.setShared(!plan.isShared());
        planRepository.save(plan);
        return new StatusCode(HttpStatus.OK, "유저 비/공개 변경 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity deleteUserPlan(Users user, String id) {
        Optional<Plans> plan = planRepository.findByUserAndId(user, Long.valueOf(id));
        if (plan.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
        }

        planLikeRepository.deleteByPlanIn(List.of(plan.get()));
        planCommentRepository.deleteByPlanIn(List.of(plan.get()));
        planRepository.delete(plan.get()); // PlanDay/PlanStop은 cascade로 같이 삭제됨
        return new StatusCode(HttpStatus.OK, "유저 플랜 삭제 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity getUserPlanById(Users user, String id) {
        Optional<Plans> plan = planRepository.findByUserAndId(user, Long.valueOf(id));
        if (plan.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "유저 단일 플랜 조회 못함").sendResponse();
        }
        return new StatusCode(HttpStatus.OK, plan.get(), "유저 단일 플랜 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity getShareMyPlan(Users user) { //공유된플랜조회
        List<Plans> shared = planRepository.findByUserAndSharedTrueOrderByIdDesc(user);
        return new StatusCode(HttpStatus.OK, shared, "공유된플랜조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity getPlan() {
        List<Plans> plans = planRepository.findBySharedTrueOrderByIdDesc();
        for (Plans plan : plans) {
            plan.setLikeCount((int) planLikeRepository.countByPlanId(plan.getId()));
        }
        return new StatusCode(HttpStatus.OK, plans, "공유된플랜보기 조회성공").sendResponse();
    }

    @Transactional
    public ResponseEntity getPlanWithPagination(String page, String size) {
        Pageable pageRequest = PageRequest.of(Integer.parseInt(page), Integer.parseInt(size));
        List<Plans> plans = planRepository.findBySharedTrue(pageRequest).getContent();
        for (Plans plan : plans) {
            plan.setLikeCount((int) planLikeRepository.countByPlanId(plan.getId()));
        }
        Long totalSize = planRepository.countBySharedTrue();
        List<Object> returnData = new ArrayList<>();
        returnData.add(totalSize);
        returnData.add(plans);
        return new StatusCode(HttpStatus.OK, returnData, "페이지네이션").sendResponse();
    }

    @Transactional
    public ResponseEntity getPlansById(String id) {
        Optional<Plans> plan = planRepository.findById(Long.valueOf(id));
        if (plan.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
        }
        plan.get().setLikeCount((int) planLikeRepository.countByPlanId(plan.get().getId()));
        return new StatusCode(HttpStatus.OK, plan.get(), "단일 플랜 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity updatePlan(Users user, Map<String, String> data) {
        try {
            Optional<Plans> resultPlan = planRepository.findById(Long.valueOf(data.get("id")));
            if (resultPlan.isEmpty()) {
                return new StatusCode(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.").sendResponse();
            }
            Plans plan = resultPlan.get();
            if (!plan.getUser().getId().equals(user.getId())) {
                // 소유자가 아니면 남의 플랜을 id로 수정할 수 없다.
                return new StatusCode(HttpStatus.FORBIDDEN, "본인의 플랜만 수정할 수 있습니다.").sendResponse();
            }
            plan.setTitle(data.get("title"));
            LocalDate[] range = parseDateRange(data.get("date"));
            plan.setStartDate(range[0]);
            plan.setEndDate(range[1]);

            // 기존 일자/장소를 지우고 새로 받은 내용으로 교체 (orphanRemoval이 지워준다)
            plan.getDays().clear();
            List<PlanDay> newDays = parseDays(data.get("plan"));
            newDays.forEach(d -> setDayParent(d, plan));
            plan.getDays().addAll(newDays);

            planRepository.save(plan);
            return new StatusCode(HttpStatus.OK, "업데이트 성공").sendResponse();
        } catch (Exception e) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
        }
    }
}
