package com.example.travel_planner.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    @Value("${app.jwt.secret-refresh}")
    private String JWT_SECRET_REFRESH;
    @Value("${app.jwt.secret-access}")
    private String JWT_SECRET_ACCESS;
    @Value("${app.jwt.secret-reset}")
    private String JWT_SECRET_RESET;

    static final int JWT_EXPIRATION_REFRESH = 604800000; // 리프레쉬 (7d)
    static final int JWT_EXPIRATION_ACCESS = 1080000; // 액세스 (3h)
    static final int JWT_EXPIRATION_RESET = 600000; // 비밀번호 재설정 (10m)

    public Map<String, String> generateToken(String email){
        Map<String, String> result = new HashMap<>();
        // 토큰 유효시간

        // jti(고유 id)가 없으면 같은 사용자에게 같은 초(second) 안에 두 번 발급될 때
        // (iat/exp가 초 단위라 이 경우 클레임이 완전히 같아짐) 토큰 문자열 자체가
        // 바이트 단위로 동일해져서, 서버에 저장해둔 해시가 충돌한다.
        String refresh_token = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email) // 사용자
                .setIssuedAt(new Date()) // 현재 시간 기반으로 생성
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_REFRESH)) // 만료 시간 세팅
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_REFRESH.getBytes()) // 사용할 암호화 알고리즘, signature에 들어갈 secret 값 세팅
                .compact();
        String access_token = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email) // 사용자
                .setIssuedAt(new Date()) // 현재 시간 기반으로 생성
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_ACCESS)) // 만료 시간 세팅
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_ACCESS.getBytes()) // 사용할 암호화 알고리즘, signature에 들어갈 secret 값 세팅
                .compact();

        result.put("refresh_token", refresh_token);
        result.put("access_token", access_token);
        return result;
    }

    // 리프레쉬 토큰에서 이메일을 꺼내주는 함수 (재발급 시 유저 부가정보 조회용)
    public String getEmailFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_REFRESH.getBytes()).parseClaimsJws(token).getBody();
            return (String) claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }

    // 비밀번호 재설정용 단기 토큰 발급 (이메일 인증코드 확인 후에만 호출되어야 함)
    public String generateResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_RESET))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_RESET.getBytes())
                .compact();
    }

    // 비밀번호 재설정 토큰에서 이메일을 꺼내며, 위변조/만료 시 null 반환
    public String getEmailFromResetToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_RESET.getBytes()).parseClaimsJws(token).getBody();
            return (String) claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }
}
