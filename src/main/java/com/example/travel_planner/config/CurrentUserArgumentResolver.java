package com.example.travel_planner.config;

import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import java.util.Optional;

// @CurrentUser Users user 파라미터를 만나면 Authorization 헤더의 액세스 토큰을
// 검증하고 그 주인인 Users를 찾아 넘겨준다. 예전엔 서비스 메서드마다
// "token.split(" ")[1] -> validateAccessToken -> getUserEmailFromToken ->
// findByEmail(...).orElseThrow()" 4줄을 반복했는데, 그걸 여기 한 곳으로 모았다.
// 토큰이 없거나/위조되었거나/만료되었거나/그 이메일의 회원이 DB에 없으면 전부
// NoSuchElementException을 던지고, Controller의 전역 @ExceptionHandler가 이걸
// "존재하지 않는 회원입니다. 다시 로그인해주세요." 401 응답으로 통일해서 내려준다.
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Value("${app.jwt.secret-access}")
    private String JWT_SECRET_ACCESS;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(Users.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String authHeader = request != null ? request.getHeader("Authorization") : null;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new NoSuchElementException("Authorization 헤더가 없습니다.");
        }

        String token = authHeader.substring("Bearer ".length());
        String email;
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_ACCESS.getBytes()).parseClaimsJws(token).getBody();
            email = claims.getSubject();
        } catch (Exception e) {
            throw new NoSuchElementException("유효하지 않거나 만료된 토큰입니다.");
        }

        return userRepository.findByEmail(email).orElseThrow();
    }

    // 비회원도 볼 수 있어야 하는 화면(예: 공유된 플랜 링크)에서, 로그인했으면 그 사용자를,
    // 안 했거나 토큰이 없거나 유효하지 않으면 조용히 빈 값을 돌려준다 - @CurrentUser와 달리
    // 여기선 "로그인 안 함"이 에러가 아니라 정상적인 케이스라서 예외를 던지지 않는다.
    public Optional<Users> resolveOptional(HttpServletRequest request) {
        String authHeader = request != null ? request.getHeader("Authorization") : null;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String token = authHeader.substring("Bearer ".length());
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_ACCESS.getBytes()).parseClaimsJws(token).getBody();
            return userRepository.findByEmail(claims.getSubject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
