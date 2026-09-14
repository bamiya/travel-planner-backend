package com.example.travel_planner.service;

import com.example.travel_planner.config.KakaoProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.UserRepository;
import com.example.travel_planner.config.JwtTokenProvider;
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
    ResourceLoader resourceLoader;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ResponseEntity getUserInfoKakao(String token) {
        KakaoProvider kakaoProvider = new KakaoProvider();

        Map userInfo = kakaoProvider.getUserInfo(token);
        if (userInfo != null) {
            Optional<Users> resultEmail = userRepository.findById((String) userInfo.get("email"));
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

    public ResponseEntity login(Map<String, String> data) {
        Optional<Users> resultEmail = userRepository.findById(data.get("email"));
        if (resultEmail.isPresent()) {
            if (!passwordEncoder.matches(data.get("pw"), resultEmail.get().getPassword())) {
                return new StatusCode(HttpStatus.NOT_FOUND, "로그인 실패! 비밀번호를 확인해주세요.").sendResponse();
            }
            Map<String, String> tokens = jwtTokenProvider.generateToken(resultEmail.get().getEmail());
            tokens.put("profileImg", resultEmail.get().getProfileImg());
            return new StatusCode(HttpStatus.OK, tokens, "로그인 성공!").sendResponse();
        }
        return new StatusCode(HttpStatus.NOT_FOUND, "로그인 실패! 아이디 또는 비밀번호를 확인해주세요.").sendResponse();
    }

    public ResponseEntity checkEmail(Map<String, String> email) {
        Optional<Users> resultEmail = userRepository.findById(email.get("email"));
        if (resultEmail.isPresent()) {
            return new StatusCode(HttpStatus.OK, "이메일이 있음").sendResponse();
        } else {
            return new StatusCode(HttpStatus.BAD_REQUEST, "없는 이메일 입니다").sendResponse();
        }
    }

    public ResponseEntity getUserInfo(String token) {
        String tokenFilter = token.split(" ")[1];
        if (jwtTokenProvider.validateAccessToken(tokenFilter)) { // 인증된 유저
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> resultEmail = userRepository.findById(getUserEmailFromToken);
            return new StatusCode(HttpStatus.OK, resultEmail, "유저 정보 조회 성공").sendResponse();
        } else {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "이미 만료된 유저임").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity getUserUpdate(String token, Map<String, String> data) {
        String tokenFilter = token.split(" ")[1];

        if (jwtTokenProvider.validateAccessToken(tokenFilter)) {
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> resultEmail = userRepository.findById(getUserEmailFromToken);

            Users users = Users.builder()
                    .email(resultEmail.get().getEmail())
                    .name(data.get("name"))
                    .birth(resultEmail.get().getBirth())
                    .password(resultEmail.get().getPassword())
                    .tel(data.get("tel"))
                    .profileImg(resultEmail.get().getProfileImg())
                    .build();
            userRepository.save(users);

            return new StatusCode(HttpStatus.OK, "회원수정성공").sendResponse();
        } else {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "회원수정실패").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity userDelete(String token){
        String tokenFilter = token.split(" ")[1];
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> resultEmail = userRepository.findById(getUserEmailFromToken);

            if(resultEmail.isPresent()){
                List<String> planList = userRepository.getIdByPlans(getUserEmailFromToken); // 해당 이메일로 된 플랜 데이터 get
                userRepository.deleteLikesByEmail(getUserEmailFromToken);    // 해당 이메일의 좋아요 목록 삭제
                userRepository.deleteCommentsByEmail(getUserEmailFromToken); // 해당 이메일의 댓글 목록 삭제
                userRepository.deletePlanListByLikes(planList); // 해당 이메일로 만든 플랜에 단 좋아요를 제거
                userRepository.deletePlanListByComments(planList); // 해당 이메일로 만든 플랜에 단 댓글을 제거
                userRepository.deletePlansById(getUserEmailFromToken); // 해당 이메일로 만든 플랜을 제거
                userRepository.delete(resultEmail.get());
            } else {
                return new StatusCode(HttpStatus.BAD_REQUEST, "존재하지 않는 회원입니다.").sendResponse();
            }
            return new StatusCode(HttpStatus.OK, "회원탈퇴성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "회원탈퇴실패").sendResponse();
        }
    }


    @Transactional
    public ResponseEntity getUserUpdatePw(String token, Map<String, String> data){
        String tokenFilter = token.split(" ")[1];
        //회원 수정란 비밀번호 변경임 안에 내용 수정해야함
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String getUserEmailFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> resultEmail = userRepository.findById(getUserEmailFromToken);
            String pw  = data.get("pw");
            String dbPw = resultEmail.get().getPassword();

            if (passwordEncoder.matches(pw, dbPw)) {
                Users users = Users.builder()
                        .email(resultEmail.get().getEmail())
                        .name(resultEmail.get().getName())
                        .birth(resultEmail.get().getBirth())
                        .password(passwordEncoder.encode(data.get("newPw")))
                        .tel(resultEmail.get().getTel())
                        .profileImg(resultEmail.get().getProfileImg())
                        .build();
                userRepository.save(users);
                return new StatusCode(HttpStatus.OK, "비밀번호변경 성공").sendResponse();
            } else {
                return new StatusCode(HttpStatus.BAD_REQUEST, "비밀번호 불일치").sendResponse();
            }
        } else {
            return new StatusCode(HttpStatus.UNAUTHORIZED,"만료된 토큰").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity register(Users user) {
        try {
            // 이메일 중복 검사
            if (!userRepository.findByEmail(user.getEmail()).isEmpty()) {
                return new StatusCode(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일 입니다.").sendResponse();
            }

            // 존재하지 않는 이메일일시 해당 계정정보 저장 (비밀번호는 반드시 해싱하여 저장)
            Users hashedUser = Users.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .birth(user.getBirth())
                    .password(passwordEncoder.encode(user.getPassword()))
                    .tel(user.getTel())
                    .profileImg(user.getProfileImg())
                    .build();
            userRepository.save(hashedUser);
            return new StatusCode(HttpStatus.OK, "회원 가입이 완료되었습니다!").sendResponse();
        } catch (Exception e) {
            return new StatusCode(HttpStatus.BAD_REQUEST, "서버에 에러가 발생했습니다.").sendResponse();
        }
    }

    public ResponseEntity getTokenUsedRefreshToken(Map<String, String> data){
        Map<String, String> token = jwtTokenProvider.generateAccessToken(data.get("refreshToken"));

        if(token.get("access_token") != null){ // 성공적으로 재발급이 됨
            return new StatusCode(HttpStatus.OK, token, "액세스 토큰 재발급 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.INTERNAL_SERVER_ERROR, "리프레쉬 토큰이 만료되었거나, 알 수 없는 에러").sendResponse();
        }
    }

    public ResponseEntity uploadFile(MultipartFile file, String token){
        String tokenFilter = token.split(" ")[1];
        if (!jwtTokenProvider.validateAccessToken(tokenFilter)) { // 인증된 유저
            return new StatusCode(HttpStatus.UNAUTHORIZED, "토큰 만료").sendResponse();
        }
        Optional<Users> email = userRepository.findById(jwtTokenProvider.getUserEmailFromToken(tokenFilter));

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

            Users users = Users.builder()
                    .email(email.get().getEmail())
                    .name(email.get().getName())
                    .birth(email.get().getBirth())
                    .password(email.get().getPassword())
                    .tel(email.get().getTel())
                    .profileImg(storedFileName)
                    .build();
            userRepository.save(users);
        } catch (Exception e) {
            e.printStackTrace();
            return new StatusCode(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 실패").sendResponse();
        }

        return new StatusCode(HttpStatus.OK, "업로드 성공").sendResponse();
    }

    // 비밀번호 찾기: 반드시 이메일 인증코드 확인 후 발급된 resetToken을 통해서만 변경 가능
    @Transactional
    public ResponseEntity passwordChange(Map<String, String> data) {
        String email = jwtTokenProvider.getEmailFromResetToken(data.get("resetToken"));
        if (email == null) {
            return new StatusCode(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 시도해주세요.").sendResponse();
        }

        Optional<Users> resultEmail = userRepository.findById(email);
        if (resultEmail.isPresent()) {
            Users users = Users.builder()
                    .email(resultEmail.get().getEmail())
                    .name(resultEmail.get().getName())
                    .birth(resultEmail.get().getBirth())
                    .password(passwordEncoder.encode(data.get("pw")))
                    .tel(resultEmail.get().getTel())
                    .profileImg(resultEmail.get().getProfileImg())
                    .build();
            userRepository.save(users);
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
