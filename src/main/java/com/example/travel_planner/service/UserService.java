package com.example.travel_planner.service;

import com.example.travel_planner.config.KakaoProvider;
import com.example.travel_planner.config.LoginAttemptService;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.PersonalInfoHistory;
import com.example.travel_planner.entity.RefreshToken;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PersonalInfoHistoryRepository;
import com.example.travel_planner.repository.PlanCommentRepository;
import com.example.travel_planner.repository.PlanLikeRepository;
import com.example.travel_planner.repository.PlanRepository;
import com.example.travel_planner.repository.RefreshTokenRepository;
import com.example.travel_planner.repository.TourCommentRepository;
import com.example.travel_planner.repository.TourLikeRepository;
import com.example.travel_planner.repository.UserRepository;
import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.entity.Plans;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private TourCommentRepository tourCommentRepository;
    @Autowired
    private PlanCommentRepository planCommentRepository;
    @Autowired
    private TourLikeRepository tourLikeRepository;
    @Autowired
    private PlanLikeRepository planLikeRepository;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersonalInfoHistoryRepository personalInfoHistoryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    // JwtTokenProvider와 만료시간 상수가 겹치지만, 그쪽은 JWT 서명에만 관여하고 DB 기록의
    // 만료시각 계산은 서비스 레이어 책임이라 별도로 둔다.
    private static final long REFRESH_TOKEN_VALIDITY_MS = 604800000L; // 7일
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.kakao.client-id}")
    private String kakaoClientId;
    @Value("${app.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${app.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    // 개인정보의 안전성 확보조치 기준 제8조(접속기록 보관) 대응용 - 실제 변경된 값은
    // 남기지 않고(최소수집), 언제/누가/무슨 처리를 했는지만 남긴다.
    private void logPersonalInfoAction(String email, PersonalInfoHistory.ActionType actionType) {
        personalInfoHistoryRepository.save(
                PersonalInfoHistory.builder()
                        .email(email)
                        .actionType(actionType)
                        .ipAddress(getClientIp())
                        .build()
        );
    }

    private String getClientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // 리프레시 토큰 원문은 저장하지 않고 해시만 저장한다 (DB 유출 시에도 토큰 재사용 방지).
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256은 모든 JVM에 기본 내장되어 있어 실제로는 발생하지 않는다
        }
    }

    // 로그인/토큰재발급 시 발급한 리프레시 토큰을 서버 기록에 남긴다 - 이 기록이 있는
    // 토큰만 재발급에 사용할 수 있다 (로그아웃/재발급 시 폐기하면 그 즉시 무효가 된다).
    private void saveRefreshToken(Users user, String rawRefreshToken) {
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(hashToken(rawRefreshToken))
                        .expiresAt(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_VALIDITY_MS / 1000))
                        .build()
        );
    }

    // 리프레시 토큰을 JS에서 읽을 수 없는 httpOnly 쿠키로 내려준다 - localStorage에 두면
    // XSS 한 번으로 그대로 탈취당할 수 있는데, httpOnly 쿠키는 스크립트가 접근할 수 없다.
    // SameSite=Strict: 이 쿠키는 우리 프론트가 fetch/axios로만 쓰고, 다른 사이트 링크를
    // 타고 들어올 때(top-level navigation) 실려야 할 이유가 없다 - CSRF 표면을 최소화한다.
    private void setRefreshTokenCookie(String rawRefreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(REFRESH_TOKEN_VALIDITY_MS / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getRefreshTokenFromCookie() {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    public ResponseEntity<?> getUserInfoKakao(String code) {
        KakaoProvider kakaoProvider = new KakaoProvider();

        String kakaoAccessToken = kakaoProvider.exchangeCodeForToken(code, kakaoClientId, kakaoClientSecret, kakaoRedirectUri);
        if (kakaoAccessToken == null) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다.").sendResponse();
        }

        Map<String, Object> userInfo = kakaoProvider.getUserInfo(kakaoAccessToken);
        if (userInfo != null) {
            String email = (String) userInfo.get("email");
            Optional<Users> resultEmail = email != null ? userRepository.findByEmail(email) : Optional.empty();
            if (resultEmail.isPresent()) {
                Map<String, String> tokens = jwtTokenProvider.generateToken(resultEmail.get().getEmail());
                saveRefreshToken(resultEmail.get(), tokens.get("refresh_token"));
                setRefreshTokenCookie(tokens.remove("refresh_token"));
                tokens.put("isUser", "Y");
                tokens.put("profileImg", resultEmail.get().getProfileImg());
                return new StatusCode(HttpStatus.OK, tokens, "로그인 성공!").sendResponse();
            }
            userInfo.put("isUser", "N");
            return new StatusCode(HttpStatus.OK, userInfo, "유저 정보 조회 성공").sendResponse();
        }
        return new StatusCode(HttpStatus.UNAUTHORIZED, "알 수 없는 오류로 잠시 후 로그인을 시도해주세요.").sendResponse();
    }

    public ResponseEntity<?> login(Map<String, String> data) {
        String email = data.get("email");

        if (loginAttemptService.isLocked(email)) {
            long remain = loginAttemptService.getLockRemainingSeconds(email);
            return new StatusCode(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. " + remain + "초 후 다시 시도해주세요.").sendResponse();
        }

        Optional<Users> resultEmail = userRepository.findByEmail(email);
        if (resultEmail.isPresent()) {
            if (resultEmail.get().getPassword() == null || !passwordEncoder.matches(data.get("pw"), resultEmail.get().getPassword())) {
                loginAttemptService.loginFailed(email);
                return new StatusCode(HttpStatus.NOT_FOUND, "로그인 실패! 비밀번호를 확인해주세요.").sendResponse();
            }
            loginAttemptService.loginSucceeded(email);
            Map<String, String> tokens = jwtTokenProvider.generateToken(resultEmail.get().getEmail());
            saveRefreshToken(resultEmail.get(), tokens.get("refresh_token"));
            setRefreshTokenCookie(tokens.remove("refresh_token"));
            tokens.put("profileImg", resultEmail.get().getProfileImg());
            return new StatusCode(HttpStatus.OK, tokens, "로그인 성공!").sendResponse();
        }
        loginAttemptService.loginFailed(email); // 존재하지 않는 이메일도 시도 횟수에 포함해 계정 존재 여부가 타이밍으로 드러나지 않게 한다
        return new StatusCode(HttpStatus.NOT_FOUND, "로그인 실패! 아이디 또는 비밀번호를 확인해주세요.").sendResponse();
    }

    public ResponseEntity<?> checkEmail(Map<String, String> email) {
        Optional<Users> resultEmail = userRepository.findByEmail(email.get("email"));
        if (resultEmail.isPresent()) {
            return new StatusCode(HttpStatus.OK, "이메일이 있음").sendResponse();
        } else {
            return new StatusCode(HttpStatus.BAD_REQUEST, "없는 이메일 입니다").sendResponse();
        }
    }

    public ResponseEntity<?> getUserInfo(Users user) {
        return new StatusCode(HttpStatus.OK, user, "유저 정보 조회 성공").sendResponse();
    }

    // 댓글 작성자 닉네임을 클릭했을 때 보여줄 간단한 공개 프로필 - 닉네임 외의 개인정보는
    // 절대 포함하지 않는다(id/email 등은 애초에 Users 응답에서 Owner 뷰로만 나가지만,
    // 여기서도 필요한 값만 직접 골라 응답한다).
    public ResponseEntity<?> getPublicProfile(String nickname) {
        Optional<Users> result = userRepository.findByNickname(nickname);
        if (result.isEmpty()) {
            return new StatusCode(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.").sendResponse();
        }
        Users user = result.get();
        long planCount = planRepository.countByUserAndSharedTrue(user);
        long commentCount = planCommentRepository.countByUser(user) + tourCommentRepository.countByUser(user);

        Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("nickname", user.getDisplayNickname());
        profile.put("profileImg", user.getProfileImg());
        profile.put("planCount", planCount);
        profile.put("commentCount", commentCount);
        return new StatusCode(HttpStatus.OK, profile, "공개 프로필 조회 성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> getUserUpdate(Users user, Map<String, String> data) {
        user.setName(data.get("name"));
        if (data.containsKey("nickname")) user.setNickname(data.get("nickname"));
        user.setTel(data.get("tel"));
        if (data.containsKey("zipcode")) user.setZipcode(data.get("zipcode"));
        if (data.containsKey("address1")) user.setAddress1(data.get("address1"));
        if (data.containsKey("address2")) user.setAddress2(data.get("address2"));
        userRepository.save(user);
        logPersonalInfoAction(user.getEmail(), PersonalInfoHistory.ActionType.UPDATE_INFO);

        return new StatusCode(HttpStatus.OK, "회원수정성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> userDelete(Users user){
        String email = user.getEmail(); // 탈퇴 후에도 이력에 남겨야 하므로 삭제 전에 스냅샷
        List<Plans> myPlans = planRepository.findByUserOrderByIdDesc(user);

        tourLikeRepository.deleteByUser(user);
        planLikeRepository.deleteByUser(user);
        tourCommentRepository.deleteByUser(user);
        planCommentRepository.deleteByUser(user);
        refreshTokenRepository.deleteByUser(user);

        // 내가 만든 플랜에 남의 좋아요/댓글이 달려있을 수 있으니 그것도 정리
        planLikeRepository.deleteByPlanIn(myPlans);
        planCommentRepository.deleteByPlanIn(myPlans);
        planRepository.deleteAll(myPlans); // Plans -> PlanDay -> PlanStop cascade

        userRepository.delete(user);
        logPersonalInfoAction(email, PersonalInfoHistory.ActionType.WITHDRAW);
        return new StatusCode(HttpStatus.OK, "회원탈퇴성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> getUserUpdatePw(Users user, Map<String, String> data){
        String pw = data.get("pw");
        String dbPw = user.getPassword();

        if (dbPw != null && passwordEncoder.matches(pw, dbPw)) {
            user.setPassword(passwordEncoder.encode(data.get("newPw")));
            userRepository.save(user);
            logPersonalInfoAction(user.getEmail(), PersonalInfoHistory.ActionType.UPDATE_PASSWORD);
            return new StatusCode(HttpStatus.OK, "비밀번호변경 성공").sendResponse();
        } else {
            return new StatusCode(HttpStatus.BAD_REQUEST, "비밀번호 불일치").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity<?> register(Map<String, String> data) {
        try {
            String email = data.get("email");
            // 이메일 중복 검사
            if (userRepository.findByEmail(email).isPresent()) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일 입니다.").sendResponse();
            }
            String nickname = data.get("nickname");
            if (nickname == null || nickname.isBlank()) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요.").sendResponse();
            }
            if (userRepository.findByNickname(nickname).isPresent()) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다.").sendResponse();
            }

            // 클라이언트가 보낼 수 있는 값 중 허용된 필드만 골라서 저장한다 (role 등은 절대 여기서 받지 않는다).
            Users hashedUser = Users.builder()
                    .email(email)
                    .name(data.get("name"))
                    .nickname(nickname)
                    .birth(data.get("birth") != null ? java.time.LocalDate.parse(data.get("birth")) : null)
                    .password(passwordEncoder.encode(data.get("password")))
                    .tel(data.get("tel"))
                    .profileImg(data.get("profileImg"))
                    .provider(Users.Provider.LOCAL)
                    .build();
            userRepository.save(hashedUser);
            logPersonalInfoAction(email, PersonalInfoHistory.ActionType.SIGN_UP);
            return new StatusCode(HttpStatus.OK, "회원 가입이 완료되었습니다!").sendResponse();
        } catch (Exception e) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity<?> getTokenUsedRefreshToken(){
        String incomingToken = getRefreshTokenFromCookie();
        String email = incomingToken != null ? jwtTokenProvider.getEmailFromRefreshToken(incomingToken) : null;
        if (email == null) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "리프레쉬 토큰이 만료되었거나, 알 수 없는 에러").sendResponse();
        }

        // 서명/만료가 유효해도, 서버 기록에 없는(이미 재발급으로 폐기됐거나 로그아웃된)
        // 토큰이면 거부한다 - 이게 없으면 탈취된 토큰이 만료 전까지 계속 재사용될 수 있다.
        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hashToken(incomingToken));
        if (stored.isEmpty()) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료되었거나 무효화된 세션입니다. 다시 로그인해주세요.").sendResponse();
        }
        Users user = stored.get().getUser();
        refreshTokenRepository.delete(stored.get()); // rotation: 쓰고 난 리프레시 토큰은 즉시 폐기

        Map<String, String> tokens = jwtTokenProvider.generateToken(email);
        saveRefreshToken(user, tokens.get("refresh_token"));
        setRefreshTokenCookie(tokens.remove("refresh_token"));
        tokens.put("profileImg", user.getProfileImg());
        return new StatusCode(HttpStatus.OK, tokens, "액세스 토큰 재발급 성공").sendResponse();
    }

    // 로그아웃: 쿠키로 들고 있는 리프레시 토큰을 서버 기록에서 지우고, 브라우저의 쿠키도
    // 지운다. 액세스 토큰이 이미 만료된 상태에서도 호출할 수 있어야 하므로 인증을 요구하지
    // 않는다 - 어차피 자기 것이 아닌 토큰 해시로는 아무 것도 지울 수 없다.
    @Transactional
    public ResponseEntity<?> logout() {
        String refreshToken = getRefreshTokenFromCookie();
        if (refreshToken != null) {
            refreshTokenRepository.deleteByTokenHash(hashToken(refreshToken));
        }
        clearRefreshTokenCookie();
        return new StatusCode(HttpStatus.OK, "로그아웃 되었습니다.").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> uploadFile(MultipartFile file, Users user){
        // file image 가 없을 경우
        if (file.isEmpty()) {
            return new StatusCode(HttpStatus.OK, "업로드 성공").sendResponse();
        }

        // 원본 파일명에서 경로 구분자 등을 제거해 path traversal을 막고, 타임스탬프로 충돌을 방지
        String safeOriginalName = Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "file"))
                .getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = System.currentTimeMillis() + "_" + safeOriginalName;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path dest = uploadPath.resolve(storedFileName).normalize();
            if (!dest.startsWith(uploadPath)) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.").sendResponse();
            }
            file.transferTo(dest);

            user.setProfileImg(storedFileName);
            userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            return new StatusCode(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 실패").sendResponse();
        }

        // 프론트가 재로그인 없이 즉시 화면에 반영할 수 있도록 새 파일명을 응답에 담아준다.
        return new StatusCode(HttpStatus.OK, storedFileName, "업로드 성공").sendResponse();
    }

    // 비밀번호 찾기: 반드시 이메일 인증코드 확인 후 발급된 resetToken을 통해서만 변경 가능
    @Transactional
    public ResponseEntity<?> passwordChange(Map<String, String> data) {
        String email = jwtTokenProvider.getEmailFromResetToken(data.get("resetToken"));
        if (email == null) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 시도해주세요.").sendResponse();
        }

        Optional<Users> resultEmail = userRepository.findByEmail(email);
        if (resultEmail.isPresent()) {
            Users user = resultEmail.get();
            user.setPassword(passwordEncoder.encode(data.get("pw")));
            userRepository.save(user);
            logPersonalInfoAction(user.getEmail(), PersonalInfoHistory.ActionType.UPDATE_PASSWORD);
            return new StatusCode(HttpStatus.OK, "비밀번호 변경").sendResponse();

        } else {
            return new StatusCode(HttpStatus.BAD_REQUEST, "없는 이메일 입니다").sendResponse();
        }
    }

    public byte[] getImage(String value) throws IOException {
        // 사용자 입력을 경로에 그대로 붙이면 path traversal이 가능하므로,
        // 파일명만 추출하고 업로드 디렉토리 밖으로 벗어나는지 검증한다.
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String safeName = Paths.get(value).getFileName().toString();
        Path target = uploadPath.resolve(safeName).normalize();
        if (!target.startsWith(uploadPath)) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }

        try (InputStream imageStream = Files.newInputStream(target)) {
            return IOUtils.toByteArray(imageStream);
        }
    }
}
