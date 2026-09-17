package com.example.travel_planner.service;

import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Notice;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    public ResponseEntity<?> getNotices() {
        // 상용화 전이라 목록이 많지 않을 것 - 최신순으로 최대 100개만 내려준다.
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100, Sort.unsorted()));
        return new StatusCode(HttpStatus.OK, notices, "공지사항 목록 조회 성공").sendResponse();
    }

    public ResponseEntity<?> getNoticeById(String id) {
        Optional<Notice> notice = noticeRepository.findById(Long.valueOf(id));
        if (notice.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.").sendResponse();
        }
        return new StatusCode(HttpStatus.OK, notice.get(), "공지사항 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> createNotice(Users user, Map<String, String> data) {
        if (user.getRole() != Users.Role.ADMIN) {
            return new StatusCode(HttpStatus.FORBIDDEN, "관리자만 공지사항을 작성할 수 있습니다.").sendResponse();
        }
        Notice notice = Notice.builder()
                .title(data.get("title"))
                .content(data.get("content"))
                .author(user)
                .build();
        noticeRepository.save(notice);
        return new StatusCode(HttpStatus.OK, "공지사항이 등록되었습니다.").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> updateNotice(Users user, Map<String, String> data) {
        if (user.getRole() != Users.Role.ADMIN) {
            return new StatusCode(HttpStatus.FORBIDDEN, "관리자만 공지사항을 수정할 수 있습니다.").sendResponse();
        }
        Optional<Notice> result = noticeRepository.findById(Long.valueOf(data.get("id")));
        if (result.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.").sendResponse();
        }
        Notice notice = result.get();
        notice.setTitle(data.get("title"));
        notice.setContent(data.get("content"));
        noticeRepository.save(notice);
        return new StatusCode(HttpStatus.OK, "공지사항이 수정되었습니다.").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> deleteNotice(Users user, String id) {
        if (user.getRole() != Users.Role.ADMIN) {
            return new StatusCode(HttpStatus.FORBIDDEN, "관리자만 공지사항을 삭제할 수 있습니다.").sendResponse();
        }
        Optional<Notice> result = noticeRepository.findById(Long.valueOf(id));
        if (result.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.").sendResponse();
        }
        noticeRepository.delete(result.get());
        return new StatusCode(HttpStatus.OK, "공지사항이 삭제되었습니다.").sendResponse();
    }
}
