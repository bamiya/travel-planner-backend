package com.example.travel_planner.service;

import com.example.travel_planner.config.KakaoProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.PlanCommentRepository;
import com.example.travel_planner.repository.PlanLikeRepository;
import com.example.travel_planner.repository.PlanRepository;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ResponseEntity<?> getUserInfoKakao(String token) {
        KakaoProvider kakaoProvider = new KakaoProvider();

        Map<String, Object> userInfo = kakaoProvider.getUserInfo(token);
        if (userInfo != null) {
            String email = (String) userInfo.get("email");
            Optional<Users> resultEmail = email != null ? userRepository.findByEmail(email) : Optional.empty();
            if (resultEmail.isPresent()) {
                Map<String, String> tokens = jwtTokenProvider.generateToken(resultEmail.get().getEmail());
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
        Optional<Users> resultEmail = userRepository.findByEmail(data.get("email"));
        if (resultEmail.isPresent()) {
            if (resultEmail.get().getPassword() == null || !passwordEncoder.matches(data.get("pw"), resultEmail.get().getPassword())) {
                return new StatusCode(HttpStatus.NOT_FOUND, "로그인 실패! 비밀번호를 확인해주세요.").sendResponse();
            }
            Map<String, String> tokens = jwtTokenProvider.generateToken(resultEmail.get().getEmail());
            tokens.put("profileImg", resultEmail.get().getProfileImg());
            return new StatusCode(HttpStatus.OK, tokens, "로그인 성공!").sendResponse();
        }
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

    @Transactional
    public ResponseEntity<?> getUserUpdate(Users user, Map<String, String> data) {
        user.setName(data.get("name"));
        user.setTel(data.get("tel"));
        if (data.containsKey("zipcode")) user.setZipcode(data.get("zipcode"));
        if (data.containsKey("address1")) user.setAddress1(data.get("address1"));
        if (data.containsKey("address2")) user.setAddress2(data.get("address2"));
        userRepository.save(user);

        return new StatusCode(HttpStatus.OK, "회원수정성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> userDelete(Users user){
        List<Plans> myPlans = planRepository.findByUserOrderByIdDesc(user);

        tourLikeRepository.deleteByUser(user);
        planLikeRepository.deleteByUser(user);
        tourCommentRepository.deleteByUser(user);
        planCommentRepository.deleteByUser(user);

        // 내가 만든 플랜에 남의 좋아요/댓글이 달려있을 수 있으니 그것도 정리
        planLikeRepository.deleteByPlanIn(myPlans);
        planCommentRepository.deleteByPlanIn(myPlans);
        planRepository.deleteAll(myPlans); // Plans -> PlanDay -> PlanStop cascade

        userRepository.delete(user);
        return new StatusCode(HttpStatus.OK, "회원탈퇴성공").sendResponse();
    }

    @Transactional
    public ResponseEntity<?> getUserUpdatePw(Users user, Map<String, String> data){
        String pw = data.get("pw");
        String dbPw = user.getPassword();

        if (dbPw != null && passwordEncoder.matches(pw, dbPw)) {
            user.setPassword(passwordEncoder.encode(data.get("newPw")));
            userRepository.save(user);
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

            // 클라이언트가 보낼 수 있는 값 중 허용된 필드만 골라서 저장한다 (role 등은 절대 여기서 받지 않는다).
            Users hashedUser = Users.builder()
                    .email(email)
                    .name(data.get("name"))
                    .birth(data.get("birth") != null ? java.time.LocalDate.parse(data.get("birth")) : null)
                    .password(passwordEncoder.encode(data.get("password")))
                    .tel(data.get("tel"))
                    .profileImg(data.get("profileImg"))
                    .provider(Users.Provider.LOCAL)
                    .build();
            userRepository.save(hashedUser);
            return new StatusCode(HttpStatus.OK, "회원 가입이 완료되었습니다!").sendResponse();
        } catch (Exception e) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
        }
    }

    public ResponseEntity<?> getTokenUsedRefreshToken(Map<String, String> data){
        Map<String, String> token = jwtTokenProvider.generateAccessToken(data.get("refreshToken"));

        if(token.get("access_token") != null){ // 성공적으로 재발급이 됨
            String email = jwtTokenProvider.getEmailFromRefreshToken(data.get("refreshToken"));
            userRepository.findByEmail(email).ifPresent(u -> token.put("profileImg", u.getProfileImg()));
            return new StatusCode(HttpStatus.OK, token, "액세스 토큰 재발급 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.INTERNAL_SERVER_ERROR, "리프레쉬 토큰이 만료되었거나, 알 수 없는 에러").sendResponse();
        }
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

        return new StatusCode(HttpStatus.OK, "업로드 성공").sendResponse();
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
