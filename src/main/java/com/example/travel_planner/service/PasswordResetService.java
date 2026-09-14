package com.example.travel_planner.service;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 비밀번호를 찾을 때, 이메일 소유자 본인인지 인증코드로 확인한 뒤에만
// 비밀번호 변경용 토큰을 내어주기 위한 서비스.
@Service
public class PasswordResetService {
    private static final long CODE_TTL_MILLIS = 5 * 60 * 1000; // 5분
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, CodeEntry> codesByEmail = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private JavaMailSender mailSender;

    public ResponseEntity sendResetCode(String email) {
        if (userRepository.findById(email).isEmpty()) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "없는 이메일 입니다").sendResponse();
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codesByEmail.put(email, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL_MILLIS));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[여행플래너] 비밀번호 재설정 인증코드");
            message.setText("인증코드: " + code + " (5분간 유효합니다)");
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println(e);
            return new StatusCode(HttpStatus.INTERNAL_SERVER_ERROR, "인증코드 발송에 실패했습니다.").sendResponse();
        }

        return new StatusCode(HttpStatus.OK, "인증코드를 발송했습니다.").sendResponse();
    }

    public ResponseEntity verifyResetCode(String email, String code) {
        CodeEntry entry = codesByEmail.get(email);
        if (entry == null || entry.isExpired() || !entry.code.equals(code)) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않거나 만료되었습니다.").sendResponse();
        }
        codesByEmail.remove(email); // 1회용

        String resetToken = jwtTokenProvider.generateResetToken(email);
        return new StatusCode(HttpStatus.OK, Map.of("resetToken", resetToken), "인증 성공").sendResponse();
    }

    private static class CodeEntry {
        final String code;
        final long expiresAt;

        CodeEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
